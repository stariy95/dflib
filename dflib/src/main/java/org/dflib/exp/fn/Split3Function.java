package org.dflib.exp.fn;

import org.dflib.Exp;
import org.dflib.Udf3;

import static org.dflib.exp.fn.FnSupport.constantValue;

public class Split3Function implements Udf3<String, String, Integer, String[]> {
    @Override
    public Exp<String[]> call(Exp<String> exp, @Constant Exp<String> regex, @Constant Exp<Integer> limit) {
        return exp.castAsStr().split(constantValue(regex), constantValue(limit));
    }
}
