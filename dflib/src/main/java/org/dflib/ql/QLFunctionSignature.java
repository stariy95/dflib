package org.dflib.ql;

import org.dflib.Exp;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * A description of a QL function that a {@link QLFunctionDescriptor} is built from: its return type, its declared
 * parameters and the factory that turns a list of argument expressions into an expression.
 * <p>
 * Every registration path ends here. A {@code Udf0..UdfN} lambda is reflected by {@link #udf0(Udf0)} and friends,
 * one {@code call} overload of a {@link QLFunction} class by {@link #reflectCall(Method)}, and the fluent form
 * ({@link #signature()}, {@link #returning(TypeClassifier)}, {@link #arg(TypeClassifier)}, ...) is what the
 * registry's own tests use to declare a function of an arbitrary shape.
 * <p>
 * Package-private on purpose: it is only promoted to public API if and when users need to register functions whose
 * shape reflection can not express.
 */
class QLFunctionSignature {

    private TypeClassifier returnType;
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

    // --- Udf0..UdfN lambdas ---

    static QLFunctionSignature udf0(Udf0<?> function) {
        return reflect(
                callMethod(function), false,
                exps -> function.call());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static QLFunctionSignature udf1(Udf1<?, ?> function) {
        return reflect(
                callMethod(function, Exp.class), false,
                exps -> function.call((Exp) exps.getFirst()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static QLFunctionSignature udf2(Udf2<?, ?, ?> function) {
        return reflect(
                callMethod(function, Exp.class, Exp.class), false,
                exps -> function.call((Exp) exps.getFirst(), (Exp) exps.get(1)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static QLFunctionSignature udf3(Udf3<?, ?, ?, ?> function) {
        return reflect(
                callMethod(function, Exp.class, Exp.class, Exp.class), false,
                exps -> function.call((Exp) exps.getFirst(), (Exp) exps.get(1), (Exp) exps.get(2)));
    }

    static QLFunctionSignature udfN(UdfN<?> function) {
        return reflect(
                callMethod(function, Exp[].class), true,
                exps -> function.call(exps.toArray(new Exp[0])));
    }

    /**
     * Builds a signature by reflecting on a UDF "call" method. Parameter and return types are recovered from the
     * method generics; {@link Constant}-annotated parameters become constant args.
     *
     * @param varArgs whether the method's single {@code Exp[]} parameter is a vararg list rather than a declared
     *                parameter
     */
    static QLFunctionSignature reflect(Method method, boolean varArgs, Function<List<Exp<?>>, Exp<?>> producer) {

        QLFunctionSignature signature = signature()
                .returning(TypeClassifier.classify(method.getGenericReturnType()))
                .as(producer);

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
     * Returns the {@code call} methods declared in a class, skipping the bridge and synthetic ones: a covariant
     * return makes javac emit a bridge {@code call} with the same erased parameters that carries neither the
     * generic types nor the parameter annotations.
     */
    private static List<Method> callMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        for (Method m : type.getDeclaredMethods()) {
            if ("call".equals(m.getName()) && !m.isBridge() && !m.isSynthetic()) {
                methods.add(m);
            }
        }
        return methods;
    }

    private static Method callMethod(Object function, Class<?>... parameterTypes) {
        Class<?> type = function.getClass();

        for (Method m : callMethods(type)) {
            if (Arrays.equals(m.getParameterTypes(), parameterTypes)) {
                return m;
            }
        }

        throw new RuntimeException(new NoSuchMethodException(
                type.getName() + ".call(" + Arrays.toString(parameterTypes) + ")"));
    }

    // --- QLFunction classes ---

    /**
     * Orders the overloads of a {@link QLFunction} so that registration - which is the resolver's tie-break among
     * equally specific candidates - does not depend on the unspecified order of
     * {@link Class#getDeclaredMethods()}.
     */
    private static final Comparator<QLFunctionSignature> OVERLOAD_ORDER = Comparator
            .<QLFunctionSignature>comparingInt(s -> s.args.size())
            .thenComparing(QLFunctionSignature::argTypeOrdinals, Arrays::compare)
            .thenComparing(QLFunctionSignature::argConstancy, Arrays::compare)
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
        for (Method m : callMethods(type)) {
            if (Modifier.isPublic(m.getModifiers())) {
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
     * Builds a signature by reflecting on one {@code call} overload of a {@link QLFunction}. Unlike the
     * {@code Udf} path, which describes a single lambda, this one holds the overload to the stricter contract
     * documented on {@link QLFunction}: an exact expression return type, and parameters that are either expressions
     * or implicit constants of an allowed Java type.
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
        if (raw == Exp.class && classifier.isTyped()) {
            throw new IllegalArgumentException("A QLFunction 'call' method returning " + generic
                    + " would claim to produce " + classifier + " while producing a bare Exp. Declare the Exp"
                    + " subinterface actually produced (e.g. StrExp), or Exp<?>: " + method);
        }

        return classifier;
    }

    private static Arg callArg(Method method, Parameter parameter) {

        Class<?> raw = parameter.getType();

        if (Exp.class.isAssignableFrom(raw)) {
            return Arg.of(parameter);
        }

        // a type variable bounded by Number erases to Number and is allowed through that entry
        if (!CallProducer.CONSTANT_TYPES.contains(raw)) {
            throw new IllegalArgumentException("A non-Exp parameter of a QLFunction is an implicit constant"
                    + " argument, and must be declared as one of " + CallProducer.CONSTANT_TYPES.stream()
                    .map(Class::getSimpleName).distinct().toList() + ", got " + raw.getName() + ": " + method);
        }

        return new Arg(TypeClassifier.classify(raw), true);
    }

    private int[] argTypeOrdinals() {
        return args.stream().mapToInt(a -> a.type().ordinal()).toArray();
    }

    private int[] argConstancy() {
        return args.stream().mapToInt(a -> a.constant() ? 1 : 0).toArray();
    }

    // --- the fluent form ---

    private QLFunctionSignature() {
    }

    /**
     * Declares the type of the expression the function produces.
     */
    QLFunctionSignature returning(TypeClassifier type) {
        this.returnType = type;
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

        if (returnType == null) {
            throw new IllegalArgumentException("No return type defined for function: " + name);
        }
    }
}
