package org.dflib.exp.fn;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.Udf1;

public class CastAsBoolFunction implements Udf1<Object, Boolean> {
    @Override
    public Condition call(Exp<Object> exp) {
        return exp.castAsBool();
    }
}
