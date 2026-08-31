package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code vConcat(e, delimiter)} and its filtered, prefixed and suffixed forms: an aggregate concatenating the
 * values of a receiver of any type into a single String.
 * <p>
 * Every arity delegates to the four-argument {@code Exp.vConcat}, defaulting the filter to null and the prefix and
 * the suffix to an empty String, as the grammar has always done. The one- and two-argument {@code Exp} API
 * overloads build a different expression. The result is a plain String-valued expression rather than a
 * {@code StrExp}.
 *
 * @since 2.0.0
 */
public class VConcatFunction implements QLFunction {

    public Exp<?> call(Exp<?> e, String delimiter) {
        return e.vConcat(null, delimiter, "", "");
    }

    public Exp<?> call(Exp<?> e, Condition filter, String delimiter) {
        return e.vConcat(filter, delimiter, "", "");
    }

    public Exp<?> call(Exp<?> e, String delimiter, String prefix, String suffix) {
        return e.vConcat(null, delimiter, prefix, suffix);
    }

    public Exp<?> call(Exp<?> e, Condition filter, String delimiter, String prefix, String suffix) {
        return e.vConcat(filter, delimiter, prefix, suffix);
    }
}
