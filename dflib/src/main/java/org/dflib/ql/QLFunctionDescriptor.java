package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.exp.ScalarExp;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class QLFunctionDescriptor {

    final String name;
    final TypeClassifier returnType;
    final Arg[] args;
    final boolean varArgs;
    final Function<List<Exp<?>>, Exp<?>> fnExpProducer;

    QLFunctionDescriptor(String name, QLFunctionSignature signature) {
        signature.validate(name);

        this.name = name;
        this.returnType = signature.returnType();
        this.args = signature.args();
        this.varArgs = signature.isVarArgs();
        this.fnExpProducer = signature.producer();
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
     * Returns the type of the expression a call to this function produces. A function whose result type depends on
     * the receiver is registered as one descriptor per receiver type, each with its own return type.
     */
    public TypeClassifier returnType() {
        return returnType;
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

    /**
     * Returns the wrapper class of a primitive type, or the type itself if it is not a primitive.
     */
    static Class<?> box(Class<?> type) {
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
        NUMERIC("castAsInt"),
        STRING("castAsStr"),
        BOOLEAN("castAsBool"),
        DATE("castAsDate"),
        TIME("castAsTime"),
        DATETIME("castAsDateTime"),
        OFFSETDATETIME("castAsOffsetDateTime"),

        /**
         * A type that is known to be a plain Object.
         */
        OBJECT(null),

        /**
         * A type that is not known statically and is only resolved at eval time, e.g. an untyped column reference.
         */
        ANY(null);

        /**
         * A result indicating that an argument can not be passed as a declared parameter.
         */
        public static final int NO_MATCH = -1;

        /**
         * The cost of an argument whose type is exactly the declared one.
         */
        public static final int EXACT = 0;

        /**
         * The cost of an argument passed to an OBJECT parameter, i.e. one written to accept any type.
         */
        public static final int WILDCARD = 1;

        /**
         * The cost of an argument whose type is only known at eval time passed to a typed parameter: it resolves,
         * but the producer may still reject it.
         */
        public static final int COERCION = 2;

        private final String castFunction;

        TypeClassifier(String castFunction) {
            this.castFunction = castFunction;
        }

        /**
         * Returns true for a classifier that names a concrete type the grammar has an expression rule for. OBJECT
         * and ANY are not types but the absence of one.
         */
        public boolean isTyped() {
            return castFunction != null;
        }

        /**
         * Returns the name of the QL cast function producing an expression of this type, or null for OBJECT and
         * ANY, which a caller can not cast to.
         */
        public String castFunction() {
            return castFunction;
        }

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
         * {@code declared}: {@link #EXACT}, {@link #WILDCARD} or {@link #COERCION}, or {@link #NO_MATCH} if the
         * argument can not be passed at all.
         */
        public static int matchCost(TypeClassifier declared, TypeClassifier actual) {

            if (declared == actual) {
                return EXACT;
            }

            if (declared == OBJECT) {
                return WILDCARD;
            }

            if (actual == ANY) {
                return COERCION;
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
