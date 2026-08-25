package org.dflib.exp.fn;

import org.dflib.Exp;
import org.dflib.Series;
import org.dflib.exp.ScalarExp;

class FnSupport {

    private FnSupport() {
    }

    /**
     * Returns the value of a constant expression.
     *
     * @throws IllegalArgumentException if the expression is not a constant
     * @see ScalarExp
     */
    static <T> T constantValue(Exp<T> exp) {
        if (exp instanceof ScalarExp) {
            return exp.reduce((Series<?>) null);
        }

        throw new IllegalArgumentException("Not a constant expression: " + exp.toQL());
    }
}
