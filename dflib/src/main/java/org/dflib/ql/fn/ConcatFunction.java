package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.StrExp;
import org.dflib.ql.QLFunction;

/**
 * QL {@code concat(..)}: String concatenation of any mix of expressions of any type. The no-argument overload
 * documents the arity of {@code concat()} and spares the resolver a fallback to the vararg one; both produce the
 * same expression.
 *
 * @since 2.0.0
 */
public class ConcatFunction implements QLFunction {

    public StrExp call() {
        return Exp.concat();
    }

    public StrExp call(Exp<?>... exps) {
        return Exp.concat((Object[]) exps);
    }
}
