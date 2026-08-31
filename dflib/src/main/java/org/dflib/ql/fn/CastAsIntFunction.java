package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code castAsInt(e)}: converts an expression of any type to an int one.
 *
 * @since 2.0.0
 */
public class CastAsIntFunction implements QLFunction {

    public NumExp<Integer> call(Exp<?> e) {
        return e.castAsInt();
    }
}
