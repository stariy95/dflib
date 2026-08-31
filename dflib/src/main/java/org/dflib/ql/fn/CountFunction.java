package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code count()} and {@code count(filter)}: an aggregate counting the rows of the input, optionally only those
 * matching a boolean expression. Takes no receiver.
 *
 * @since 2.0.0
 */
public class CountFunction implements QLFunction {

    public NumExp<Integer> call() {
        return Exp.count();
    }

    public NumExp<Integer> call(Condition filter) {
        return Exp.count(filter);
    }
}
