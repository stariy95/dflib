package org.dflib.exp.fn;

import org.dflib.Exp;
import org.dflib.Udf2;

import static org.dflib.exp.fn.FnSupport.constantValue;

public class Substr2Function implements Udf2<Object, Integer, String> {
    @Override
    public Exp<String> call(Exp<Object> exp1, @Constant Exp<Integer> exp2) {
        return exp1.substr(constantValue(exp2));
    }
}
