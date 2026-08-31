package org.dflib.ql.fn;

import org.dflib.DateTimeExp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code second(e)}: the second of a time, datetime or offset datetime expression. A date has no time part, so a
 * {@code DateExp} receiver is rejected.
 *
 * @since 2.0.0
 */
public class SecondFunction implements QLFunction {

    public NumExp<Integer> call(TimeExp e) {
        return e.second();
    }

    public NumExp<Integer> call(DateTimeExp e) {
        return e.second();
    }

    public NumExp<Integer> call(OffsetDateTimeExp e) {
        return e.second();
    }
}
