package org.dflib.exp.fn;

import org.dflib.Exp;
import org.dflib.Udf2;

import static org.dflib.exp.fn.FnSupport.constantValue;

public class Split2Function implements Udf2<String, String, String[]> {
    @Override
    public Exp<String[]> call(Exp<String> exp, @Constant Exp<String> regex) {
        return exp.castAsStr().split(constantValue(regex));
    }
}
