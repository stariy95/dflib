package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code first(e)} and {@code first(e, filter)}: an aggregate returning the first value of a receiver of any
 * type, optionally of the rows matching a boolean expression. The result carries its value type only at eval time,
 * so it implements none of the typed expression interfaces.
 *
 * @since 2.0.0
 */
public class FirstFunction implements QLFunction {

    public <T> Exp<T> call(Exp<T> e) {
        return e.first();
    }

    public <T> Exp<T> call(Exp<T> e, Condition filter) {
        return e.first(filter);
    }
}
