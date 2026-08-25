package org.dflib.exp.fn;

import org.dflib.Exp;
import org.dflib.Udf3;

import static org.dflib.exp.fn.FnSupport.constantValue;

public class Substr3Function implements Udf3<Object, Integer, Integer, String> {
    @Override
    public Exp<String> call(Exp<Object> exp1, @Constant Exp<Integer> exp2, @Constant Exp<Integer> exp3) {
        return exp1.substr(constantValue(exp2), constantValue(exp3));
    }
}
