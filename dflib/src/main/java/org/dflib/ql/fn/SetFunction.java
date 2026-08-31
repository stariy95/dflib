package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code set(e)}: an aggregate collecting the values of a receiver of any type into a Set. The result is a
 * Set-valued expression with no dedicated type of its own.
 *
 * @since 2.0.0
 */
public class SetFunction implements QLFunction {

    public Exp<?> call(Exp<?> e) {
        return e.set();
    }
}
