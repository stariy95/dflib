package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.StrExp;
import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

/**
 * A fluent, non-reflective description of a QL function: its return type, its declared parameters and the factory
 * that turns a list of argument expressions into an expression.
 * <p>
 * This is the single way a {@link QLFunctionDescriptor} is assembled. Both reflective registration paths are
 * implemented on top of it - {@link #reflect(Method, boolean)} for a {@code Udf0..UdfN} lambda, and
 * {@link #reflectCall(Method)} for one {@code call} overload of a {@link QLFunction} - so there is only one
 * descriptor construction code path.
 * <p>
 * Built-in functions no longer go through it directly: they are {@link QLFunction} classes in
 * {@code org.dflib.ql.fn}, whose overloads are reflected. What remains of the explicit path is the fluent form used
 * by the registry's own tests and by hand-built registrations, including {@link #returningArgType(int)} - a return
 * type that follows an argument's type, which reflection can not express and which no built-in needs.
 * <p>
 * Package-private on purpose: it is only promoted to public API if and when users need to register functions whose
 * shape reflection can not express.
 */
class QLFunctionSignature {

    /**
     * A {@link #returnArgIndex} value meaning "the return type does not depend on the arguments".
     */
    static final int FIXED_RETURN = -1;

    private TypeClassifier returnType;
    private int returnArgIndex = FIXED_RETURN;
    private final List<Arg> args = new ArrayList<>();
    private boolean varArgs;
    private Function<List<Exp<?>>, Exp<?>> producer;

    /**
     * The {@code call} overload this signature was reflected from, for the {@link QLFunction} path only. Kept so
     * that the producer can be attached after the overloads have been ordered.
     */
    private Method method;

    static QLFunctionSignature signature() {
        return new QLFunctionSignature();
    }

    /**
     * Builds a signature by reflecting on a UDF "call" method. Parameter and return types are recovered from the
     * method generics; {@link Constant}-annotated parameters become constant args.
     *
     * @param varArgs whether the method's single {@code Exp[]} parameter is a vararg list rather than a declared
     *                parameter
     */
    static QLFunctionSignature reflect(Method method, boolean varArgs) {

        QLFunctionSignature signature = signature()
                .returning(TypeClassifier.classify(method.getGenericReturnType()));

        Parameter[] parameters = method.getParameters();

        if (varArgs) {
            // varargs declare no individual arguments, so a constant marker on them would be silently dropped
            for (Parameter p : parameters) {
                if (p.isAnnotationPresent(Constant.class)) {
                    throw new IllegalArgumentException(
                            "Vararg functions declare no arguments, so none can be @Constant: " + method);
                }
            }

            signature.varArgs();
        } else {
            for (Parameter p : parameters) {
                signature.arg(Arg.of(p));
            }
        }

        return signature;
    }

    /**
     * The return classifiers a typed expression rule of the grammar can ask about. A bare {@code Exp<V>} return
     * whose value type lands on one of these is a lie: it claims the type without producing the interface that
     * carries it.
     */
    private static final EnumSet<TypeClassifier> TYPED_CLASSIFIERS = EnumSet.of(
            TypeClassifier.NUMERIC,
            TypeClassifier.STRING,
            TypeClassifier.BOOLEAN,
            TypeClassifier.DATE,
            TypeClassifier.TIME,
            TypeClassifier.DATETIME,
            TypeClassifier.OFFSETDATETIME);

    /**
     * Orders the overloads of a {@link QLFunction} so that registration - which is the resolver's tie-break among
     * equally specific candidates - does not depend on the unspecified order of
     * {@link Class#getDeclaredMethods()}.
     */
    private static final Comparator<QLFunctionSignature> OVERLOAD_ORDER = Comparator
            .<QLFunctionSignature>comparingInt(s -> s.args.size())
            .thenComparing(QLFunctionSignature::argTypeOrdinals)
            .thenComparing(QLFunctionSignature::argConstancy)
            .thenComparing(s -> s.varArgs)
            .thenComparingInt(s -> s.returnType.ordinal());

    /**
     * Reflects all the signatures of a {@link QLFunction}: one per public {@code call} method declared in its class,
     * in a deterministic order.
     */
    static List<QLFunctionSignature> reflectQLFunction(String name, QLFunction function) {

        Class<?> type = function.getClass();

        if (function instanceof Udf0 || function instanceof Udf1 || function instanceof Udf2
                || function instanceof Udf3 || function instanceof UdfN) {
            throw new IllegalArgumentException("A QLFunction must not also implement Udf0..UdfN, as their default"
                    + " methods are public 'call' methods and would register as bogus signatures: " + type.getName());
        }

        if (!Modifier.isPublic(type.getModifiers())) {
            throw new IllegalArgumentException("A QLFunction class must be public to be invoked reflectively: "
                    + type.getName());
        }

        List<QLFunctionSignature> signatures = new ArrayList<>();
        for (Method m : type.getDeclaredMethods()) {
            if ("call".equals(m.getName())
                    && Modifier.isPublic(m.getModifiers())
                    && !m.isBridge()
                    && !m.isSynthetic()) {

                signatures.add(reflectCall(m));
            }
        }

        if (signatures.isEmpty()) {
            throw new IllegalArgumentException(
                    "A QLFunction must declare at least one public 'call' method: " + type.getName());
        }

        signatures.sort(OVERLOAD_ORDER);
        for (QLFunctionSignature s : signatures) {
            s.as(new CallProducer(name, function, s.method));
        }

        return signatures;
    }

    /**
     * Builds a signature by reflecting on one {@code call} overload of a {@link QLFunction}. Unlike
     * {@link #reflect(Method, boolean)}, which describes a single {@code Udf} lambda, this path holds the overload to
     * the stricter contract documented on {@link QLFunction}: an exact expression return type, and parameters that
     * are either expressions or implicit constants of an allowed Java type.
     */
    static QLFunctionSignature reflectCall(Method method) {

        QLFunctionSignature signature = signature().returning(returnClassifier(method));
        signature.method = method;

        Parameter[] parameters = method.getParameters();
        boolean varArgs = method.isVarArgs();
        int declared = varArgs ? parameters.length - 1 : parameters.length;

        for (int i = 0; i < declared; i++) {
            signature.arg(callArg(method, parameters[i]));
        }

        if (varArgs) {
            Parameter tail = parameters[parameters.length - 1];

            // varargs declare no individual arguments, so a marker on them would be silently dropped
            if (tail.isAnnotationPresent(Constant.class)) {
                throw new IllegalArgumentException(
                        "A vararg parameter declares no argument, so it can not be @Constant: " + method);
            }
            if (tail.isAnnotationPresent(Cast.class)) {
                throw new IllegalArgumentException(
                        "A vararg parameter declares no argument, so it can not be @Cast: " + method);
            }

            Class<?> component = tail.getType().getComponentType();
            if (component == null || !Exp.class.isAssignableFrom(component)) {
                throw new IllegalArgumentException(
                        "A vararg parameter of a QLFunction must be an Exp array: " + method);
            }

            signature.varArgs();
        }

        return signature;
    }

    private static TypeClassifier returnClassifier(Method method) {

        Class<?> raw = method.getReturnType();
        if (!Exp.class.isAssignableFrom(raw)) {
            throw new IllegalArgumentException(
                    "A QLFunction 'call' method must return an Exp, got " + raw.getName() + ": " + method);
        }

        Type generic = method.getGenericReturnType();
        TypeClassifier classifier = TypeClassifier.classify(generic);

        // "Exp<String>" classifies as STRING while producing an expression that is not a StrExp: the parser would
        // record the call as a string one and the cast at the call site would fail. "Exp<?>" and "<T> Exp<T>"
        // classify as OBJECT and are fine
        if (raw == Exp.class && TYPED_CLASSIFIERS.contains(classifier)) {
            throw new IllegalArgumentException("A QLFunction 'call' method returning " + generic
                    + " would claim to produce " + classifier + " while producing a bare Exp. Declare the Exp"
                    + " subinterface actually produced (e.g. StrExp), or Exp<?>: " + method);
        }

        return classifier;
    }

    private static Arg callArg(Method method, Parameter parameter) {

        Class<?> raw = parameter.getType();
        boolean cast = parameter.isAnnotationPresent(Cast.class);

        if (Exp.class.isAssignableFrom(raw)) {
            if (cast && !CallProducer.CASTS.containsKey(raw)) {
                throw new IllegalArgumentException("@Cast is only supported for " + StrExp.class.getSimpleName()
                        + " and " + Condition.class.getSimpleName() + " parameters, as no total cast to "
                        + raw.getSimpleName() + " exists: " + method);
            }

            return new Arg(
                    TypeClassifier.classify(parameter.getParameterizedType()),
                    parameter.isAnnotationPresent(Constant.class));
        }

        if (cast) {
            throw new IllegalArgumentException(
                    "@Cast is only supported for expression parameters: " + method);
        }

        // a type variable bounded by Number erases to Number and is allowed through that entry
        if (!CallProducer.CONSTANT_TYPES.contains(raw)) {
            throw new IllegalArgumentException("A non-Exp parameter of a QLFunction is an implicit constant"
                    + " argument, and must be declared as one of " + CallProducer.CONSTANT_TYPES.stream()
                    .map(Class::getSimpleName).distinct().toList() + ", got " + raw.getName() + ": " + method);
        }

        return new Arg(TypeClassifier.classify(raw), true);
    }

    private String argTypeOrdinals() {
        StringBuilder out = new StringBuilder();
        for (Arg a : args) {
            out.append((char) ('0' + a.type().ordinal()));
        }
        return out.toString();
    }

    private String argConstancy() {
        StringBuilder out = new StringBuilder();
        for (Arg a : args) {
            out.append(a.constant() ? '1' : '0');
        }
        return out.toString();
    }

    private QLFunctionSignature() {
    }

    /**
     * Declares a return type that does not depend on the arguments.
     */
    QLFunctionSignature returning(TypeClassifier type) {
        this.returnType = type;
        this.returnArgIndex = FIXED_RETURN;
        return this;
    }

    /**
     * Declares a polymorphic function whose return type is the type of the argument at the given position, e.g.
     * {@code shift(e, n)} or {@code min(e)}.
     */
    QLFunctionSignature returningArgType(int argIndex) {
        if (argIndex < 0) {
            throw new IllegalArgumentException("Negative return arg index: " + argIndex);
        }
        this.returnType = null;
        this.returnArgIndex = argIndex;
        return this;
    }

    /**
     * Appends a declared parameter that accepts any expression of the given type.
     */
    QLFunctionSignature arg(TypeClassifier type) {
        return arg(new Arg(type, false));
    }

    /**
     * Appends a declared parameter that only accepts a constant (scalar) expression of the given type. Such
     * arguments can be read at parse time.
     */
    QLFunctionSignature constArg(TypeClassifier type) {
        return arg(new Arg(type, true));
    }

    QLFunctionSignature arg(Arg arg) {
        this.args.add(arg);
        return this;
    }

    /**
     * Marks the function as accepting a variable number of trailing arguments. Any parameters declared with
     * {@link #arg(TypeClassifier)} / {@link #constArg(TypeClassifier)} become leading typed parameters that must
     * still be present and match; anything past them is unconstrained.
     */
    QLFunctionSignature varArgs() {
        this.varArgs = true;
        return this;
    }

    /**
     * Sets the factory producing an expression from the call arguments.
     */
    QLFunctionSignature as(Function<List<Exp<?>>, Exp<?>> producer) {
        this.producer = producer;
        return this;
    }

    TypeClassifier returnType() {
        return returnType;
    }

    int returnArgIndex() {
        return returnArgIndex;
    }

    Arg[] args() {
        return args.toArray(new Arg[0]);
    }

    boolean isVarArgs() {
        return varArgs;
    }

    Function<List<Exp<?>>, Exp<?>> producer() {
        return producer;
    }

    void validate(String name) {
        if (producer == null) {
            throw new IllegalArgumentException("No expression producer defined for function: " + name);
        }

        if (returnArgIndex == FIXED_RETURN) {
            if (returnType == null) {
                throw new IllegalArgumentException("No return type defined for function: " + name);
            }
        } else if (returnArgIndex >= args.size()) {
            throw new IllegalArgumentException("Function " + name + " returns the type of argument "
                    + returnArgIndex + ", but declares only " + args.size() + " argument(s)");
        }
    }
}
