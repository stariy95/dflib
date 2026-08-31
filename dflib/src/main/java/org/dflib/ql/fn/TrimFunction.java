package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.StrExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code trim(e)}: converts each value to a String and removes its leading and trailing spaces. Accepts a
 * receiver of any type.
 *
 * @since 2.0.0
 */
public class TrimFunction implements QLFunction {

    public StrExp call(Exp<?> e) {
        return e.trim();
    }
}
