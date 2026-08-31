package org.dflib.ql.fn;

import org.dflib.DateTimeExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code plusHours(e, n)}: adds a constant number of hours to a temporal expression, preserving the receiver
 * type. A {@code DateExp} receiver is rejected: a date has no time part.
 *
 * @since 2.0.0
 */
public class PlusHoursFunction implements QLFunction {

    public TimeExp call(TimeExp e, int n) {
        return e.plusHours(n);
    }

    public DateTimeExp call(DateTimeExp e, int n) {
        return e.plusHours(n);
    }

    public OffsetDateTimeExp call(OffsetDateTimeExp e, int n) {
        return e.plusHours(n);
    }
}
