package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code castAsTime(e)} and {@code castAsTime(e, format)}: converts an expression of any type to a time one,
 * parsing its String form with an optional constant format pattern.
 *
 * @since 2.0.0
 */
public class CastAsTimeFunction implements QLFunction {

    public TimeExp call(Exp<?> e) {
        return e.castAsTime();
    }

    public TimeExp call(Exp<?> e, String format) {
        return e.castAsTime(format);
    }
}
