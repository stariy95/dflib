package org.dflib.ql.fn;

import org.dflib.NumExp;
import org.dflib.StrExp;
import org.dflib.ql.Cast;
import org.dflib.ql.QLFunction;

/**
 * QL {@code len(e)}: the length of each value as a String. Accepts a string receiver, and casts one whose type is
 * only known at eval time; a receiver of another known type is rejected.
 *
 * @since 2.0.0
 */
public class LenFunction implements QLFunction {

    public NumExp<Integer> call(@Cast StrExp e) {
        return e.len();
    }
}
