package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.Series;
import org.dflib.exp.ScalarExp;

import java.math.BigInteger;

/**
 * Reads the values of constant (scalar) argument expressions.
 */
class ConstantArgs {

    private ConstantArgs() {
    }

    /**
     * Returns the value of a constant expression.
     *
     * @throws IllegalArgumentException if the expression is not a constant
     */
    static <T> T constantValue(Exp<T> exp) {
        if (exp instanceof ScalarExp) {
            return exp.reduce((Series<?>) null);
        }

        throw new IllegalArgumentException("Not a constant expression: " + (exp != null ? exp.toQL() : "null"));
    }

    /**
     * Narrows an integral constant to an int, rejecting fractional literals rather than truncating them.
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
