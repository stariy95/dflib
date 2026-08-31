package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.NumExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code min(e)} and {@code min(e, filter)}: an aggregate returning the smallest value of the receiver,
 * optionally over the rows matching a boolean expression. The result is of the receiver's own type. An
 * {@code OffsetDateTimeExp} receiver is rejected: it declares no aggregates.
 *
 * @since 2.0.0
 */
public class MinFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e) {
        return e.min();
    }

    public NumExp<?> call(NumExp<?> e, Condition filter) {
        return e.min(filter);
    }

    public StrExp call(StrExp e) {
        return e.min();
    }

    public StrExp call(StrExp e, Condition filter) {
        return e.min(filter);
    }

    public DateExp call(DateExp e) {
        return e.min();
    }

    public DateExp call(DateExp e, Condition filter) {
        return e.min(filter);
    }

    public TimeExp call(TimeExp e) {
        return e.min();
    }

    public TimeExp call(TimeExp e, Condition filter) {
        return e.min(filter);
    }

    public DateTimeExp call(DateTimeExp e) {
        return e.min();
    }

    public DateTimeExp call(DateTimeExp e, Condition filter) {
        return e.min(filter);
    }
}
