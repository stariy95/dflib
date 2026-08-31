package org.dflib.ql.fn;

import org.dflib.DecimalExp;
import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code castAsDecimal(e)}: converts an expression of any type to a BigDecimal one.
 *
 * @since 2.0.0
 */
public class CastAsDecimalFunction implements QLFunction {

    public DecimalExp call(Exp<?> e) {
        return e.castAsDecimal();
    }
}
