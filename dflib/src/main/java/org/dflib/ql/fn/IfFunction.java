package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code if(condition, ifTrue, ifFalse)}: picks one of two expressions of any type per row. The result carries
 * its value type only at eval time, so it implements none of the typed expression interfaces.
 *
 * @since 2.0.0
 */
public class IfFunction implements QLFunction {

    public <T> Exp<T> call(Condition condition, Exp<T> ifTrue, Exp<T> ifFalse) {
        return Exp.ifExp(condition, ifTrue, ifFalse);
    }
}
