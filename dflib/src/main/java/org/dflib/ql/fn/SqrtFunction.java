package org.dflib.ql.fn;

import org.dflib.NumExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code sqrt(e)}: the square root of a numeric expression.
 *
 * @since 2.0.0
 */
public class SqrtFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e) {
        return e.sqrt();
    }
}
