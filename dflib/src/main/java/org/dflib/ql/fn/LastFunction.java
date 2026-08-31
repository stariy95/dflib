package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code last(e)}: an aggregate returning the last value of a receiver of any type. The result carries its value
 * type only at eval time, so it implements none of the typed expression interfaces. The {@code Exp} API declares no
 * filtered overload.
 *
 * @since 2.0.0
 */
public class LastFunction implements QLFunction {

    public <T> Exp<T> call(Exp<T> e) {
        return e.last();
    }
}
