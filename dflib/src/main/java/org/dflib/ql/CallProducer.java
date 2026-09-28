package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.Series;
import org.dflib.exp.ScalarExp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Produces an expression by invoking one {@code call} overload of a {@link QLFunction}, unwrapping the constant
 * arguments to the declared parameter types. The resolver has already matched the argument types against the
 * declared ones, so the only checks left here are the ones it can not express: an integer parameter given a
 * fractional constant, and an expression parameter narrower than its classifier (e.g. {@code DecimalExp}).
 */
class CallProducer implements Function<List<Exp<?>>, Exp<?>> {

    /**
     * Java types a non-expression (implicit constant) parameter may be declared as.
     */
    static final List<Class<?>> CONSTANT_TYPES = List.of(
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            boolean.class, Boolean.class,
            String.class,
            Number.class,
            LocalDate.class, LocalTime.class, LocalDateTime.class, OffsetDateTime.class,
            Object.class);

    private final String name;
    private final QLFunction function;
    private final Method method;
    private final Class<?>[] paramTypes;
    private final boolean varArgs;

    CallProducer(String name, QLFunction function, Method method) {
        this.name = name;
        this.function = function;
        this.method = method;
        this.varArgs = method.isVarArgs();

        Parameter[] parameters = method.getParameters();
        int declared = varArgs ? parameters.length - 1 : parameters.length;

        this.paramTypes = new Class<?>[declared];
        for (int i = 0; i < declared; i++) {
            paramTypes[i] = parameters[i].getType();
        }
    }

    @Override
    public Exp<?> apply(List<Exp<?>> args) {

        int declared = paramTypes.length;
        Object[] values = new Object[declared + (varArgs ? 1 : 0)];

        for (int i = 0; i < declared; i++) {
            values[i] = Exp.class.isAssignableFrom(paramTypes[i]) ? expArg(i, args.get(i)) : constantArg(i, args.get(i));
        }

        if (varArgs) {
            values[declared] = args.subList(declared, args.size()).toArray(new Exp[0]);
        }

        try {
            return (Exp<?>) method.invoke(function, values);
        } catch (InvocationTargetException e) {
            // unwrap, or the parser reports a generic "Unexpected exception during parsing"
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Error calling " + name + "(): " + cause.getMessage(), cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Function " + name + "() is not accessible: " + method, e);
        }
    }

    private Exp<?> expArg(int i, Exp<?> arg) {

        if (paramTypes[i].isInstance(arg)) {
            return arg;
        }

        throw new IllegalArgumentException(name + "() expects argument " + (i + 1) + " to be a "
                + paramTypes[i].getSimpleName() + ", got: " + arg.toQL());
    }

    private Object constantArg(int i, Exp<?> arg) {

        Class<?> declared = paramTypes[i];
        Object value = ((ScalarExp<?>) arg).reduce((Series<?>) null);

        if (declared == int.class || declared == Integer.class) {
            return integerConstant((Number) value, arg).intValue();
        }

        if (declared == long.class || declared == Long.class) {
            return integerConstant((Number) value, arg).longValue();
        }

        if (declared == double.class || declared == Double.class) {
            return ((Number) value).doubleValue();
        }

        return value;
    }

    private static Number integerConstant(Number n, Exp<?> exp) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte || n instanceof BigInteger) {
            return n;
        }

        throw new IllegalArgumentException("Not an integer constant: " + exp.toQL());
    }
}
