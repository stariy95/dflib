package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;
import org.dflib.exp.ScalarExp;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class QLFunctionDescriptor {

    final String name;

    /**
     * A return type that does not depend on the arguments, or null for a polymorphic function
     * (see {@link #returnArgIndex}).
     */
    final TypeClassifier returnType;

    /**
     * An index of the argument whose type is the return type of this function, or
     * {@link QLFunctionSignature#FIXED_RETURN} if the return type is fixed.
     */
    final int returnArgIndex;

    final Arg[] args;
    final boolean varArgs;
    final Function<List<Exp<?>>, Exp<?>> fnExpProducer;

    private final EnumSet<TypeClassifier> possibleReturnTypes;

    QLFunctionDescriptor(String name, QLFunctionSignature signature) {
        signature.validate(name);

        this.name = name;
        this.returnType = signature.returnType();
        this.returnArgIndex = signature.returnArgIndex();
        this.args = signature.args();
        this.varArgs = signature.isVarArgs();
        this.fnExpProducer = signature.producer();
        this.possibleReturnTypes = possibleReturnTypes(this.returnType);
    }

    private static EnumSet<TypeClassifier> possibleReturnTypes(TypeClassifier fixedReturnType) {

        if (fixedReturnType != null) {
            return EnumSet.of(fixedReturnType);
        }

        // A polymorphic function returns the type of one of its arguments, so statically it "may return" anything.
        // ANY is excluded on purpose: it is not a type, but the absence of one, and a caller asking
        // "may this name return T?" is always asking about a concrete T it knows how to consume. A polymorphic call
        // that does resolve to ANY is reachable from the untyped expression position, which never asks.
        return EnumSet.complementOf(EnumSet.of(TypeClassifier.ANY));
    }

    public static Builder ofUdf0(Udf0<?> function) {
        return new Builder().udf0(function);
    }

    public static Builder ofUdf1(Udf1<?, ?> function) {
        return new Builder().udf1(function);
    }

    public static Builder ofUdf2(Udf2<?, ?, ?> function) {
        return new Builder().udf2(function);
    }

    public static Builder ofUdf3(Udf3<?, ?, ?, ?> function) {
        return new Builder().udf3(function);
    }

    public static Builder ofUdfN(UdfN<?> function) {
        return new Builder().udfN(function);
    }

    /**
     * Two descriptors are equal when they have the same name and the same declared argument shape. The return type
     * is deliberately excluded: function resolution is driven by the name and the arguments alone, so two
     * descriptors with an identical argument shape are genuinely unresolvable no matter what they return. Treating
     * them as equal is what turns such a pair into an error at registry build time, instead of a silent
     * first-one-wins at parse time.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        QLFunctionDescriptor that = (QLFunctionDescriptor) o;
        return name.equals(that.name)
                && varArgs == that.varArgs
                && Arrays.equals(args, that.args);
    }

    public String name() {
        return name;
    }

    /**
     * Returns the return type of this function if it does not depend on the arguments, null otherwise.
     *
     * @see #returnType(List)
     */
    public TypeClassifier returnType() {
        return returnType;
    }

    /**
     * Returns the index of the argument whose type this function returns, or
     * {@link QLFunctionSignature#FIXED_RETURN} for a function with a fixed return type.
     */
    public int returnArgIndex() {
        return returnArgIndex;
    }

    /**
     * Returns the effective return type of a call with the given arguments.
     */
    public TypeClassifier returnType(List<Arg> actualArgs) {
        return returnArgIndex == QLFunctionSignature.FIXED_RETURN
                ? returnType
                : actualArgs.get(returnArgIndex).type();
    }

    /**
     * Returns an over-approximation of the types this function may return, regardless of the arguments. Used by the
     * parser to decide whether a call by this name can possibly be parsed as an expression of a given type.
     */
    public EnumSet<TypeClassifier> possibleReturnTypes() {
        return EnumSet.copyOf(possibleReturnTypes);
    }

    public Arg[] args() {
        return args;
    }

    public boolean isVarArgs() {
        return varArgs;
    }

    public Function<List<Exp<?>>, Exp<?>> expProducer() {
        return fnExpProducer;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(args);
        result = 31 * result + Boolean.hashCode(varArgs);
        return result;
    }

    /**
     * A builder of a descriptor for a reflectively described user function. It is a thin adapter over
     * {@link QLFunctionSignature}, which is the single descriptor construction path.
     */
    public static class Builder {

        String name;
        QLFunctionSignature signature;

        private Builder() {
        }

        public Builder udf0(Udf0<?> function) {
            return signature(
                    getCallMethodSafe(function), false,
                    exps -> function.call());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder udf1(Udf1<?, ?> function) {
            return signature(
                    getCallMethodSafe(function, Exp.class), false,
                    exps -> function.call((Exp) exps.getFirst()));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder udf2(Udf2<?, ?, ?> function) {
            return signature(
                    getCallMethodSafe(function, Exp.class, Exp.class), false,
                    exps -> function.call((Exp) exps.getFirst(), (Exp) exps.get(1)));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder udf3(Udf3<?, ?, ?, ?> function) {
            return signature(
                    getCallMethodSafe(function, Exp.class, Exp.class, Exp.class), false,
                    exps -> function.call((Exp) exps.getFirst(), (Exp) exps.get(1), (Exp) exps.get(2)));
        }

        public Builder udfN(UdfN<?> function) {
            return signature(
                    getCallMethodSafe(function, Exp[].class), true,
                    exps -> function.call(exps.toArray(new Exp[0])));
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        private Builder signature(Method method, boolean varArgs, Function<List<Exp<?>>, Exp<?>> producer) {
            this.signature = QLFunctionSignature.reflect(method, varArgs).as(producer);
            return this;
        }

        QLFunctionDescriptor build() {
            return new QLFunctionDescriptor(name, signature);
        }
    }

    static private Method getCallMethodSafe(Object function, Class<?>... parameterTypes) {
        Class<?> aClass = function.getClass();

        for (Method m : aClass.getDeclaredMethods()) {
            if ("call".equals(m.getName())
                    && !m.isBridge()
                    && !m.isSynthetic()
                    && Arrays.equals(m.getParameterTypes(), parameterTypes)) {
                return m;
            }
        }

        throw new RuntimeException(new NoSuchMethodException(
                aClass.getName() + ".call(" + Arrays.toString(parameterTypes) + ")"));
    }

    /**
     * Expression interfaces whose classifier is fixed by the interface itself, whatever its value type parameter
     * reflects as. Consulted before any generic unwinding, because {@code NumExp<?>}, raw {@code NumExp} and
     * {@code <N extends Number> NumExp<N>} all carry their numeric-ness in the interface and not in the reflected
     * type argument: a wildcard's reflected upper bound is {@code Object}, a raw type has no argument at all, and
     * the interface-declared {@code N extends Number} bound is propagated into neither. Iterated in order, first
     * assignable wins; the entries are mutually disjoint except for {@link org.dflib.DecimalExp}, which is a
     * {@link NumExp}.
     */
    private static final Map<Class<?>, TypeClassifier> EXP_INTERFACE_CLASSIFIERS;

    /**
     * Primitives are boxed before classification: {@code Number.class.isAssignableFrom(int.class)} is false, so an
     * implicit constant parameter declared as {@code int} or {@code double} would otherwise classify as OBJECT.
     */
    private static final Map<Class<?>, Class<?>> BOXED_PRIMITIVES = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class);

    static {
        Map<Class<?>, TypeClassifier> m = new LinkedHashMap<>();
        m.put(NumExp.class, TypeClassifier.NUMERIC);
        m.put(StrExp.class, TypeClassifier.STRING);
        m.put(Condition.class, TypeClassifier.BOOLEAN);
        m.put(DateExp.class, TypeClassifier.DATE);
        m.put(TimeExp.class, TypeClassifier.TIME);
        m.put(DateTimeExp.class, TypeClassifier.DATETIME);
        m.put(OffsetDateTimeExp.class, TypeClassifier.OFFSETDATETIME);
        EXP_INTERFACE_CLASSIFIERS = m;
    }

    /**
     * Returns the erasure of a type if it is a class or a parameterized type, null for anything else (a type
     * variable, a wildcard, a generic array).
     */
    private static Class<?> erasedOrNull(Type type) {
        return switch (type) {
            case Class<?> c -> c;
            case ParameterizedType pt when pt.getRawType() instanceof Class<?> raw -> raw;
            case null, default -> null;
        };
    }

    private static Class<?> box(Class<?> type) {
        return type.isPrimitive() ? BOXED_PRIMITIVES.getOrDefault(type, type) : type;
    }

    private static Class<?> unwindGeneric(Type type) {
        switch (type) {
            case Class<?> c -> {
                // a non-parameterized expression interface, such as StrExp
                return Exp.class.isAssignableFrom(c) ? unwindExpType(c) : c;
            }
            case ParameterizedType pt -> {
                // unwrap the expression layer only
                return pt.getRawType() instanceof Class<?> raw && Exp.class.isAssignableFrom(raw)
                        ? unwindGeneric(pt.getActualTypeArguments()[0])
                        : unwindGeneric(pt.getRawType());
            }
            case GenericArrayType gat -> {
                Type genericComponentType = gat.getGenericComponentType();
                return unwindGeneric(genericComponentType);
            }
            case WildcardType wt -> {
                Type[] lowerBounds = wt.getLowerBounds();
                if (lowerBounds.length > 0) {
                    return unwindGeneric(lowerBounds[0]);
                }
                Type[] upperBounds = wt.getUpperBounds();
                if (upperBounds.length > 0) {
                    return unwindGeneric(upperBounds[0]);
                }
                throw new IllegalArgumentException("Wildcard type with no bounds");
            }
            case TypeVariable<?> tv -> {
                // a method-level type variable, as in "<T> Exp<T> call(..)" or "<N extends Number> N filler".
                // Reflection can not resolve it to a call site, so it is worth exactly its declared bound:
                // "T" is Object (OBJECT), "N extends Number" is Number (NUMERIC)
                Type[] bounds = tv.getBounds();
                return bounds.length > 0 ? unwindGeneric(bounds[0]) : Object.class;
            }
            case null, default
                    -> throw new IllegalArgumentException("Unexpected type " + type);
        }
    }

    /**
     * Resolves the value type of an expression.
     */
    private static Class<?> unwindExpType(Class<?> expType) {

        for (Type i : expType.getGenericInterfaces()) {
            if (i instanceof ParameterizedType pt
                    && pt.getRawType() instanceof Class<?> raw
                    && Exp.class.isAssignableFrom(raw)) {

                Type valueType = pt.getActualTypeArguments()[0];
                if (!(valueType instanceof TypeVariable)) {
                    return unwindGeneric(valueType);
                }
            }
        }

        return expType;
    }

    public enum TypeClassifier {
        NUMERIC,
        STRING,
        BOOLEAN,
        DATE,
        TIME,
        DATETIME,
        OFFSETDATETIME,

        /**
         * A type that is known to be a plain Object.
         */
        OBJECT,

        /**
         * A type that is not known statically and is only resolved at eval time, e.g. an untyped column reference.
         */
        ANY;

        /**
         * A result indicating that an argument can not be passed as a declared parameter.
         */
        public static final int NO_MATCH = -1;

        /**
         * Classifies a declared Java type: a method return type, a parameter type or the value type of an
         * expression.
         */
        public static TypeClassifier classify(Type type) {

            switch (type) {
                case TypeVariable<?> tv -> {
                    // "<T> Exp<T> call(Exp<T> e, T filler)": a bare type variable in a parameter or return
                    // position is worth its declared bound - Object for an unbounded "T", Number for
                    // "<N extends Number> N"
                    Type[] bounds = tv.getBounds();
                    return bounds.length > 0 ? classify(bounds[0]) : OBJECT;
                }
                case WildcardType wt -> {
                    Type[] lowerBounds = wt.getLowerBounds();
                    if (lowerBounds.length > 0) {
                        return classify(lowerBounds[0]);
                    }
                    Type[] upperBounds = wt.getUpperBounds();
                    return upperBounds.length > 0 ? classify(upperBounds[0]) : OBJECT;
                }
                case null, default -> {
                }
            }

            // a typed expression interface classifies by the interface alone. Its reflected type argument is
            // useless for "NumExp<?>" and absent for raw "NumExp", and unwinding it would land on Object
            Class<?> erased = erasedOrNull(type);
            if (erased != null && !erased.isArray()) {
                for (Map.Entry<Class<?>, TypeClassifier> e : EXP_INTERFACE_CLASSIFIERS.entrySet()) {
                    if (e.getKey().isAssignableFrom(erased)) {
                        return e.getValue();
                    }
                }
            }

            // what is left is a raw "Exp<V>" (or a non-expression type): classify its value type
            Class<?> expressionType = box(unwindGeneric(type));
            if (Number.class.isAssignableFrom(expressionType)) {
                return NUMERIC;
            } else if (CharSequence.class.isAssignableFrom(expressionType)) {
                return STRING;
            } else if (expressionType.equals(Boolean.class)) {
                return BOOLEAN;
            } else if (expressionType.equals(java.time.LocalDate.class)) {
                return DATE;
            } else if (expressionType.equals(java.time.LocalTime.class)) {
                return TIME;
            } else if (expressionType.equals(java.time.LocalDateTime.class)) {
                return DATETIME;
            } else if (expressionType.equals(java.time.OffsetDateTime.class)) {
                return OFFSETDATETIME;
            } else {
                return OBJECT;
            }
        }

        public static TypeClassifier classify(Exp<?> exp) {
            return switch (exp) {
                case NumExp<?> ignored -> TypeClassifier.NUMERIC;
                case StrExp ignored -> TypeClassifier.STRING;
                case Condition ignored -> TypeClassifier.BOOLEAN;
                case DateExp ignored -> TypeClassifier.DATE;
                case TimeExp ignored -> TypeClassifier.TIME;
                case DateTimeExp ignored -> TypeClassifier.DATETIME;
                case OffsetDateTimeExp ignored -> TypeClassifier.OFFSETDATETIME;
                case ScalarExp<?> ignored -> TypeClassifier.OBJECT;

                // What is left is an expression that implements none of the typed Exp interfaces: a bare column
                // ref, "if", "ifNull", "shift", "first" and friends. Its value type - when it has one - is only
                // recoverable at eval time, so it classifies as ANY and is passable to a parameter of any type
                case Exp<?> e -> classifyValueType(e.getType());

                case null -> TypeClassifier.OBJECT;
            };
        }

        private static TypeClassifier classifyValueType(Class<?> valueType) {

            if (valueType == null || valueType == Object.class) {
                // no static type at all, e.g. a bare column ref or "ifNull(a, b)" over untyped columns
                return ANY;
            }

            // "first(date(a))" is a FirstExp<LocalDate> and "if(c, int(a), int(b))" is an IfExp<Integer>: both are
            // usable as a typed argument, they just can't be recognized by their interface. Only a genuinely
            // Object-valued expression - Exp<String[]> from "split()", Exp<List<T>> from "list()" - stays OBJECT
            return classify(valueType) == OBJECT ? OBJECT : ANY;
        }

        /**
         * Returns the cost of passing an argument of the {@code actual} type to a parameter declared as
         * {@code declared}. Returns {@link #NO_MATCH} if an argument can not be passed at all.
         */
        public static int matchCost(TypeClassifier declared, TypeClassifier actual) {

            if (declared == actual) {
                return 0; // an exact match
            }

            if (declared == OBJECT) {
                return 1; // the parameter is declared to accept an argument of any type
            }

            if (actual == ANY) {
                return 2; // the argument type is only known at eval time
            }

            return NO_MATCH;
        }
    }

    /**
     * Function argument description
     */
    public record Arg(TypeClassifier type, boolean constant) {

        public static Arg of(Exp<?> exp) {
            return new Arg(TypeClassifier.classify(exp), exp instanceof ScalarExp);
        }

        static Arg of(Parameter parameter) {
            return new Arg(TypeClassifier.classify(parameter.getParameterizedType()),
                    parameter.isAnnotationPresent(Constant.class));
        }

        /**
         * Returns the cost of passing the argument to a declared parameter or {@link TypeClassifier#NO_MATCH}
         * if it can not be passed at all.
         */
        public static int matchCost(Arg declared, Arg actual) {
            if (declared.constant() && !actual.constant()) {
                return TypeClassifier.NO_MATCH;
            }
            return TypeClassifier.matchCost(declared.type(), actual.type());
        }

        @Override
        public String toString() {
            return constant ? "const " + type : type.toString();
        }
    }
}
