package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.NumExp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code avg(e)} and {@code avg(e, filter)}: an aggregate returning the mean value of the receiver, optionally
 * over the rows matching a boolean expression. The result is of the receiver's own type. Strings have no mean, and
 * {@code OffsetDateTimeExp} declares no aggregates, so neither is a valid receiver.
 *
 * @since 2.0.0
 */
public class AvgFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e) {
        return e.avg();
    }

    public NumExp<?> call(NumExp<?> e, Condition filter) {
        return e.avg(filter);
    }

    public DateExp call(DateExp e) {
        return e.avg();
    }

    public DateExp call(DateExp e, Condition filter) {
        return e.avg(filter);
    }

    public TimeExp call(TimeExp e) {
        return e.avg();
    }

    public TimeExp call(TimeExp e, Condition filter) {
        return e.avg(filter);
    }

    public DateTimeExp call(DateTimeExp e) {
        return e.avg();
    }

    public DateTimeExp call(DateTimeExp e, Condition filter) {
        return e.avg(filter);
    }
}
