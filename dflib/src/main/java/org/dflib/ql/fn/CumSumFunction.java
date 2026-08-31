package org.dflib.ql.fn;

import org.dflib.NumExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code cumSum(e)}: a running total of a numeric expression. The {@code Exp} API declares no filtered overload.
 *
 * @since 2.0.0
 */
public class CumSumFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e) {
        return e.cumSum();
    }
}
