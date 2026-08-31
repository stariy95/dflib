package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code ifNull(e, ifNull)}: replaces the null values of a receiver of any type with those of another
 * expression. The result carries its value type only at eval time, so it implements none of the typed expression
 * interfaces.
 *
 * @since 2.0.0
 */
public class IfNullFunction implements QLFunction {

    public <T> Exp<T> call(Exp<T> e, Exp<T> ifNull) {
        return Exp.ifNull(e, ifNull);
    }
}
