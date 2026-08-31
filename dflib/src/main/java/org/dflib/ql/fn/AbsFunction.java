package org.dflib.ql.fn;

import org.dflib.NumExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code abs(e)}: the absolute value of a numeric expression.
 *
 * @since 2.0.0
 */
public class AbsFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e) {
        return e.abs();
    }
}
