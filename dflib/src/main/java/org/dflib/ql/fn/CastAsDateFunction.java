package org.dflib.ql.fn;

import org.dflib.DateExp;
import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code castAsDate(e)} and {@code castAsDate(e, format)}: converts an expression of any type to a date one,
 * parsing its String form with an optional constant format pattern.
 *
 * @since 2.0.0
 */
public class CastAsDateFunction implements QLFunction {

    public DateExp call(Exp<?> e) {
        return e.castAsDate();
    }

    public DateExp call(Exp<?> e, String format) {
        return e.castAsDate(format);
    }
}
