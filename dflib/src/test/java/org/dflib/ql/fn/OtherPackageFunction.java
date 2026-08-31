package org.dflib.ql.fn;

import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.StrExp;
import org.dflib.ql.Cast;
import org.dflib.ql.QLFunction;

/**
 * A fixture proving that a public {@link QLFunction} class in a package other than {@code org.dflib.ql} - which is
 * where the built-in functions will live - is reflectively invokable without {@code setAccessible}.
 */
public class OtherPackageFunction implements QLFunction {

    public NumExp<Integer> call(@Cast StrExp e) {
        return e.len();
    }

    public NumExp<?> call(NumExp<?> e, int offset) {
        return e.shift(offset);
    }

    public StrExp call(Exp<?>... exps) {
        return Exp.concat((Object[]) exps);
    }
}
