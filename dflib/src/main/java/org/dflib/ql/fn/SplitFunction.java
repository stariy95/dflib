package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.StrExp;
import org.dflib.ql.Cast;
import org.dflib.ql.QLFunction;

/**
 * QL {@code split(e, regex)} and {@code split(e, regex, limit)}: splits each value into an array of Strings.
 * Accepts a string receiver, and casts one whose type is only known at eval time; a receiver of another known type
 * is rejected. The result is a {@code String[]}-valued expression with no dedicated type of its own.
 *
 * @since 2.0.0
 */
public class SplitFunction implements QLFunction {

    public Exp<?> call(@Cast StrExp e, String regex) {
        return e.split(regex);
    }

    public Exp<?> call(@Cast StrExp e, String regex, int limit) {
        return e.split(regex, limit);
    }
}
