package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.NumExp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code median(e)} and {@code median(e, filter)}: an aggregate returning the median value of the receiver,
 * optionally over the rows matching a boolean expression. The result is of the receiver's own type. Strings have no
 * median, and {@code OffsetDateTimeExp} declares no aggregates, so neither is a valid receiver.
 *
 * @since 2.0.0
 */
public class MedianFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e) {
        return e.median();
    }

    public NumExp<?> call(NumExp<?> e, Condition filter) {
        return e.median(filter);
    }

    public DateExp call(DateExp e) {
        return e.median();
    }

    public DateExp call(DateExp e, Condition filter) {
        return e.median(filter);
    }

    public TimeExp call(TimeExp e) {
        return e.median();
    }

    public TimeExp call(TimeExp e, Condition filter) {
        return e.median(filter);
    }

    public DateTimeExp call(DateTimeExp e) {
        return e.median();
    }

    public DateTimeExp call(DateTimeExp e, Condition filter) {
        return e.median(filter);
    }
}
