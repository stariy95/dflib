package org.dflib.ql.fn;

import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code month(e)}: the month of a date, datetime or offset datetime expression, 1-based. A time has no date
 * part, so a {@code TimeExp} receiver is rejected.
 *
 * @since 2.0.0
 */
public class MonthFunction implements QLFunction {

    public NumExp<Integer> call(DateExp e) {
        return e.month();
    }

    public NumExp<Integer> call(DateTimeExp e) {
        return e.month();
    }

    public NumExp<Integer> call(OffsetDateTimeExp e) {
        return e.month();
    }
}
