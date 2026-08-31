package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.Series;
import org.dflib.exp.ScalarExp;

import java.math.BigInteger;

/**
 * Reads the value of a constant (scalar) argument expression. Used by the reflective {@link CallProducer}, which
 * unwraps an implicit constant parameter of a {@link QLFunction} by its declared Java type, and by the rare
 * {@link QLFunction} that has to take a constant argument as an expression and unwrap it itself.
 *
 * @since 2.0.0
 */
public class ConstantArgs {

    private ConstantArgs() {
    }

    /**
     * Returns the value of a constant expression.
     *
     * @throws IllegalArgumentException if the expression is not a constant
     * @see ScalarExp
     */
    public static <T> T constantValue(Exp<T> exp) {
        if (exp instanceof ScalarExp) {
            return exp.reduce((Series<?>) null);
        }

        throw new IllegalArgumentException("Not a constant expression: " + (exp != null ? exp.toQL() : "null"));
    }

    /**
     * Narrows a constant numeric argument to an int. The QL integer literal type depends on the literal magnitude and
     * suffix (Integer, Long or BigInteger), so the value is narrowed rather than cast. A fractional literal is
     * rejected instead of being silently truncated: these arguments are counts, positions and scales, and the
     * grammar used to accept only integer literals in these positions.
     */
    static int toInt(Number n, Exp<?> exp) {
        return requireIntegral(n, exp).intValue();
    }

    static long toLong(Number n, Exp<?> exp) {
        return requireIntegral(n, exp).longValue();
    }

    private static Number requireIntegral(Number n, Exp<?> exp) {
        if (!(n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte
                || n instanceof BigInteger)) {
            throw new IllegalArgumentException("Not an integer constant: " + exp.toQL());
        }

        return n;
    }
}
