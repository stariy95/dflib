package org.dflib.ql;

import org.dflib.*;
import org.dflib.exp.ScalarExp;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static final int EXACT = 0;

    /**
     * The cost of an argument passed to an OBJECT parameter.
     */
    public static final int WILDCARD = 1;

    /**
     * The cost of an ANY argument passed to a typed parameter.
     */
    public static final int COERCION = 2;

    /**
     * Typed expression interfaces, checked in order before any generic unwinding, as their reflected type argument
     * is useless (a wildcard bound is Object, a raw type has none).
     */
    private static final Map<Class<?>, TypeClassifier> EXP_INTERFACE_CLASSIFIERS;
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

    private static final Map<Class<?>, Class<?>> BOXED_PRIMITIVES = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class
    );

    private final String castFunction;

    TypeClassifier(String castFunction) {
        this.castFunction = castFunction;
    }

    /**
     * Returns true for a classifier that names a concrete type, i.e. anything but OBJECT and ANY.
     */
    public boolean isTyped() {
        return castFunction != null;
    }

    /**
     * Returns the name of the QL cast function producing an expression of this type, or null for OBJECT and ANY.
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

        Class<?> erased = erasedOrNull(type);
        if (erased != null && !erased.isArray()) {
            for (Map.Entry<Class<?>, TypeClassifier> e : EXP_INTERFACE_CLASSIFIERS.entrySet()) {
                if (e.getKey().isAssignableFrom(erased)) {
                    return e.getValue();
                }
            }
        }

        Class<?> valueType = unwindGeneric(type);
        Class<?> expressionType = valueType.isPrimitive() ? BOXED_PRIMITIVES.getOrDefault(valueType, valueType) : valueType;
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

            // an expression implementing none of the typed interfaces ("if", "first", a bare column ref)
            case Exp<?> e -> classifyValueType(e.getType());

            case null -> TypeClassifier.OBJECT;
        };
    }

    private static TypeClassifier classifyValueType(Class<?> valueType) {

        if (valueType == null || valueType == Object.class) {
            return ANY;
        }

        return classify(valueType) == OBJECT ? OBJECT : ANY;
    }

    private static Class<?> erasedOrNull(Type type) {
        return switch (type) {
            case Class<?> c -> c;
            case ParameterizedType pt when pt.getRawType() instanceof Class<?> raw -> raw;
            case null, default -> null;
        };
    }

    private static Class<?> unwindGeneric(Type type) {
        switch (type) {
            case Class<?> c -> {
                return Exp.class.isAssignableFrom(c) ? unwindExpType(c) : c;
            }
            case ParameterizedType pt -> {
                // unwrap the expression layer only
                return pt.getRawType() instanceof Class<?> raw && Exp.class.isAssignableFrom(raw)
                        ? unwindGeneric(pt.getActualTypeArguments()[0])
                        : unwindGeneric(pt.getRawType());
            }
            case GenericArrayType gat -> {
                return unwindGeneric(gat.getGenericComponentType());
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
                // a type variable is worth its declared bound
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

    /**
     * Returns the cost of passing an argument of the {@code actual} type to a parameter declared as
     * {@code declared}, or {@link #NO_MATCH} if it can not be passed.
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
