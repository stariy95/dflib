package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.StrExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code substr(e, from)} and {@code substr(e, from, len)}: a substring of each value converted to a String.
 * Accepts a receiver of any type; the position and the length are integer constants.
 *
 * @since 2.0.0
 */
public class SubstrFunction implements QLFunction {

    public StrExp call(Exp<?> e, int fromInclusive) {
        return e.substr(fromInclusive);
    }

    public StrExp call(Exp<?> e, int fromInclusive, int len) {
        return e.substr(fromInclusive, len);
    }
}
