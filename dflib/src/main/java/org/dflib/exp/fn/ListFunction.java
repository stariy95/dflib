package org.dflib.exp.fn;

import org.dflib.Exp;
import org.dflib.Udf1;

import java.util.List;

public class ListFunction implements Udf1<Object, List<Object>> {
    @Override
    public Exp<List<Object>> call(Exp<Object> exp) {
        return exp.list();
    }
}
