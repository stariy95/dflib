package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Produces an expression by invoking one {@code call} overload of a {@link QLFunction}, checking the expression
 * arguments against the declared parameter types and unwrapping the constant ones.
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
            LocalDate.class, LocalTime.class, LocalDateTime.class, OffsetDateTime.class);

    private final String name;
    private final QLFunction function;
    private final Method method;
    private final Class<?>[] paramTypes;
    private final boolean[] isExp;
    private final Class<?> varArgComponent;

    CallProducer(String name, QLFunction function, Method method) {
        this.name = name;
        this.function = function;
        this.method = method;

        Parameter[] parameters = method.getParameters();
        int declared = method.isVarArgs() ? parameters.length - 1 : parameters.length;

        this.varArgComponent = method.isVarArgs()
                ? parameters[parameters.length - 1].getType().getComponentType()
                : null;

        this.paramTypes = new Class<?>[declared];
        this.isExp = new boolean[declared];

        for (int i = 0; i < declared; i++) {
            Class<?> type = parameters[i].getType();
            paramTypes[i] = type;
            isExp[i] = Exp.class.isAssignableFrom(type);
        }
    }

    @Override
    public Exp<?> apply(List<Exp<?>> args) {

        int declared = paramTypes.length;
        Object[] values = new Object[declared + (varArgComponent != null ? 1 : 0)];

        for (int i = 0; i < declared; i++) {
            Exp<?> arg = args.get(i);
            values[i] = isExp[i] ? expArg(i, arg) : constantArg(i, arg);
        }

        if (varArgComponent != null) {
            int tail = args.size() - declared;
            Object array = Array.newInstance(varArgComponent, tail);
            for (int i = 0; i < tail; i++) {
                Array.set(array, i, args.get(declared + i));
            }
            values[declared] = array;
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

        throw wrongArg(i, arg, "");
    }

    private Object constantArg(int i, Exp<?> arg) {

        Class<?> declared = paramTypes[i];
        Object value = ConstantArgs.constantValue(arg);

        if (declared == int.class || declared == Integer.class) {
            return ConstantArgs.toInt(requireNumber(i, arg, value), arg);
        }

        if (declared == long.class || declared == Long.class) {
            return ConstantArgs.toLong(requireNumber(i, arg, value), arg);
        }

        if (declared == double.class || declared == Double.class) {
            return requireNumber(i, arg, value).doubleValue();
        }

        if (!QLFunctionDescriptor.box(declared).isInstance(value)) {
            throw wrongArg(i, arg, "a constant ");
        }

        return value;
    }

    private Number requireNumber(int i, Exp<?> arg, Object value) {
        if (value instanceof Number n) {
            return n;
        }

        throw wrongArg(i, arg, "a constant ");
    }

    private IllegalArgumentException wrongArg(int i, Exp<?> arg, String qualifier) {
        return new IllegalArgumentException(name + "() expects argument " + (i + 1) + " to be " + qualifier
                + TypeClassifier.classify(paramTypes[i]) + ", got: " + (arg != null ? arg.toQL() : "null"));
    }
}
