package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.StrExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code upper(e)}: converts each value to a String and then to upper case. Accepts a receiver of any type.
 *
 * @since 2.0.0
 */
public class UpperFunction implements QLFunction {

    public StrExp call(Exp<?> e) {
        return e.upper();
    }
}
