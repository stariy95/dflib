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

    static <T> T constantValue(Exp<T> exp) {
        if (exp instanceof ScalarExp) {
            return exp.reduce((Series<?>) null);
        }

        throw new IllegalArgumentException("Not a constant expression: " + (exp != null ? exp.toQL() : "null"));
    }

    static int toInt(Number n, Exp<?> exp) {
        return requireIntegerType(n, exp).intValue();
    }

    static long toLong(Number n, Exp<?> exp) {
        return requireIntegerType(n, exp).longValue();
    }

    private static Number requireIntegerType(Number n, Exp<?> exp) {
        if (!(n instanceof Integer
                || n instanceof Long
                || n instanceof Short
                || n instanceof Byte
                || n instanceof BigInteger)) {
            throw new IllegalArgumentException("Not an integer constant: " + exp.toQL());
        }

        return n;
    }
}
