package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code list(e)}: an aggregate collecting the values of a receiver of any type into a List. The result is a
 * List-valued expression with no dedicated type of its own.
 *
 * @since 2.0.0
 */
public class ListFunction implements QLFunction {

    public Exp<?> call(Exp<?> e) {
        return e.list();
    }
}
