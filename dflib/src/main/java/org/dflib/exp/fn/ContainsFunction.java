package org.dflib.exp.fn;

import org.dflib.Condition;
import org.dflib.Exp;
import org.dflib.Udf2;

import static org.dflib.exp.fn.FnSupport.constantValue;

public class ContainsFunction implements Udf2<Object, String, Boolean> {
    @Override
    public Condition call(Exp<Object> exp, @Constant Exp<String> pattern) {
        return exp.contains(constantValue(pattern));
    }
}
