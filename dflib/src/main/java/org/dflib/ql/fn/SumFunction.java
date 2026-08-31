package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.NumExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code sum(e)} and {@code sum(e, filter)}: a numeric-only aggregate summing the values of the receiver. Its
 * return type is fixed, unlike the receiver-preserving {@code min}/{@code max}/{@code avg}/{@code median}.
 *
 * @since 2.0.0
 */
public class SumFunction implements QLFunction {

    public NumExp<?> call(NumExp<?> e) {
        return e.sum();
    }

    public NumExp<?> call(NumExp<?> e, Condition filter) {
        return e.sum(filter);
    }
}
