package org.dflib.exp.fn;

import org.dflib.Exp;
import org.dflib.Udf1;

import java.util.Set;

public class SetFunction implements Udf1<Object, Set<Object>> {
    @Override
    public Exp<Set<Object>> call(Exp<Object> exp) {
        return exp.set();
    }
}
