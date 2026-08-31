package org.dflib.ql.fn;

import org.dflib.NumExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code scale(e, n)}: sets the decimal scale of a numeric expression, casting it to a decimal first. The scale
 * is an integer constant.
 *
 * @since 2.0.0
 */
public class ScaleFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e, int scale) {
        return e.castAsDecimal().scale(scale);
    }
}
