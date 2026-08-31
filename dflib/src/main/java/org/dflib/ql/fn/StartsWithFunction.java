package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code startsWith(e, prefix)}: converts each value to a String and tests it for a constant prefix. Accepts a
 * receiver of any type.
 *
 * @since 2.0.0
 */
public class StartsWithFunction implements QLFunction {

    public Condition call(Exp<?> e, String prefix) {
        return e.startsWith(prefix);
    }
}
