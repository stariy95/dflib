package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;

/**
 * The type of a QL expression as seen by the function resolver: one of the typed expression kinds, or
 * {@link #OBJECT} for an expression whose type is not known statically (an untyped column reference, a "first"
 * or an "if" result, a constant of an unlisted Java type). A typed classifier knows the QL cast producing an
 * expression of its type, which the resolver applies to an untyped argument of a typed parameter.
 *
 * @since 2.0.0
 */
public enum TypeClassifier {

    // TODO: cast via "castAsNumber" once it is available (#579), so that untyped args can be passed to numeric params
    NUMERIC("castAsInt", null),
    STRING("castAsStr", Exp::castAsStr),
    BOOLEAN("castAsBool", Exp::castAsBool),
    DATE("castAsDate", Exp::castAsDate),
    TIME("castAsTime", Exp::castAsTime),
    DATETIME("castAsDateTime", Exp::castAsDateTime),
    OFFSETDATETIME("castAsOffsetDateTime", Exp::castAsOffsetDateTime),
    OBJECT(null, null);

    /**
     * A result indicating that an argument can not be passed as a declared parameter.
     */
    static final int NO_MATCH = -1;

    static final int EXACT = 0;

    /**
     * The cost of an argument passed to an OBJECT parameter.
     */
    static final int WILDCARD = 1;

    /**
     * The cost of an untyped argument cast to the type of a typed parameter.
     */
    static final int COERCION = 2;

    private static final Map<Class<?>, TypeClassifier> EXP_INTERFACES = Map.of(
            NumExp.class, NUMERIC,
            StrExp.class, STRING,
            Condition.class, BOOLEAN,
            DateExp.class, DATE,
            TimeExp.class, TIME,
            DateTimeExp.class, DATETIME,
            OffsetDateTimeExp.class, OFFSETDATETIME);

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
    private final Function<Exp<?>, Exp<?>> cast;

    TypeClassifier(String castFunction, Function<Exp<?>, Exp<?>> cast) {
        this.castFunction = castFunction;
        this.cast = cast;
    }

    /**
     * Returns true for a classifier that names a concrete type, i.e. anything but OBJECT.
     */
    public boolean isTyped() {
        return castFunction != null;
    }

    /**
     * Returns the name of the QL cast function producing an expression of this type, or null for OBJECT.
     */
    public String castFunction() {
        return castFunction;
    }

    /**
     * Returns true if an untyped expression can be cast to this type by the resolver.
     */
    public boolean canCast() {
        return cast != null;
    }

    /**
     * Casts an untyped expression to this type.
     */
    public Exp<?> cast(Exp<?> exp) {
        if (cast == null) {
            throw new IllegalStateException("No cast to " + this);
        }
        return cast.apply(exp);
    }

    /**
     * Classifies an expression by the interface it implements. An expression implementing none of the typed
     * interfaces is an OBJECT, whatever its declared value type.
     */
    public static TypeClassifier classify(Exp<?> exp) {
        return exp != null ? classifyExpInterface(exp.getClass()) : OBJECT;
    }

    private static TypeClassifier classifyExpInterface(Class<?> expType) {
        for (Map.Entry<Class<?>, TypeClassifier> e : EXP_INTERFACES.entrySet()) {
            if (e.getKey().isAssignableFrom(expType)) {
                return e.getValue();
            }
        }
        return OBJECT;
    }

    /**
     * Classifies a declared Java type: a method return type, a parameter type or a value type. An expression type
     * is classified by its interface ({@code NumExp<?>} is NUMERIC), then by its value type ({@code Exp<Integer>}
     * is NUMERIC too, {@code Exp<?>} is OBJECT). Type variables and wildcards are worth their bounds.
     */
    public static TypeClassifier classify(Type type) {
        return switch (type) {
            case Class<?> c -> classifyErased(c);
            case ParameterizedType pt -> {
                Class<?> raw = (Class<?>) pt.getRawType();
                TypeClassifier byInterface = classifyErased(raw);
                yield byInterface != OBJECT || !Exp.class.isAssignableFrom(raw)
                        ? byInterface
                        : classify(pt.getActualTypeArguments()[0]);
            }
            case GenericArrayType ignored -> OBJECT;
            case TypeVariable<?> tv -> classify(bound(tv.getBounds()));
            case WildcardType wt -> classify(wt.getLowerBounds().length > 0
                    ? wt.getLowerBounds()[0]
                    : bound(wt.getUpperBounds()));
            case null, default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };
    }

    private static Type bound(Type[] bounds) {
        return bounds.length > 0 ? bounds[0] : Object.class;
    }

    private static TypeClassifier classifyErased(Class<?> type) {

        if (Exp.class.isAssignableFrom(type)) {
            TypeClassifier byInterface = classifyExpInterface(type);

            // a class implementing "Exp<Integer>" and the like
            return byInterface != OBJECT ? byInterface : classify(declaredValueType(type));
        }

        if (type.isArray()) {
            return OBJECT;
        }

        Class<?> boxed = BOXED_PRIMITIVES.getOrDefault(type, type);

        if (Number.class.isAssignableFrom(boxed)) {
            return NUMERIC;
        } else if (CharSequence.class.isAssignableFrom(boxed)) {
            return STRING;
        } else if (boxed == Boolean.class) {
            return BOOLEAN;
        } else if (boxed == LocalDate.class) {
            return DATE;
        } else if (boxed == LocalTime.class) {
            return TIME;
        } else if (boxed == LocalDateTime.class) {
            return DATETIME;
        } else if (boxed == OffsetDateTime.class) {
            return OFFSETDATETIME;
        } else {
            return OBJECT;
        }
    }

    /**
     * Returns the value type an expression class binds in its {@code Exp} superinterface, or Object if the class
     * leaves it as a type variable.
     */
    private static Type declaredValueType(Class<?> expType) {

        for (Type i : expType.getGenericInterfaces()) {
            if (i instanceof ParameterizedType pt
                    && Exp.class.isAssignableFrom((Class<?>) pt.getRawType())) {

                Type valueType = pt.getActualTypeArguments()[0];
                if (!(valueType instanceof TypeVariable)) {
                    return valueType;
                }
            }
        }

        return Object.class;
    }

    /**
     * Returns the cost of passing an argument of the {@code actual} type to a parameter declared as
     * {@code declared}, or {@link #NO_MATCH} if it can not be passed. An untyped argument can be passed to a typed
     * parameter with a cast, at the {@link #COERCION} cost.
     */
    static int matchCost(TypeClassifier declared, TypeClassifier actual) {

        if (declared == actual) {
            return EXACT;
        }

        if (declared == OBJECT) {
            return WILDCARD;
        }

        if (actual == OBJECT && declared.canCast()) {
            return COERCION;
        }

        return NO_MATCH;
    }
}
