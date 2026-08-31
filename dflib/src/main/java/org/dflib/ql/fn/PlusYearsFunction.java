package org.dflib.ql.fn;

import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code plusYears(e, n)}: adds a constant number of years to a temporal expression, preserving the receiver
 * type. A {@code TimeExp} receiver is rejected: a time has no date part.
 *
 * @since 2.0.0
 */
public class PlusYearsFunction implements QLFunction {

    public DateExp call(DateExp e, int n) {
        return e.plusYears(n);
    }

    public DateTimeExp call(DateTimeExp e, int n) {
        return e.plusYears(n);
    }

    public OffsetDateTimeExp call(OffsetDateTimeExp e, int n) {
        return e.plusYears(n);
    }
}
