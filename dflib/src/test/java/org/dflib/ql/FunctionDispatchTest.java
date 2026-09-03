package org.dflib.ql;

import org.dflib.Exp;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.dflib.Exp.$boolVal;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.count;
import static org.dflib.Exp.parseExp;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A prediction cached by ANTLR under a name-dependent predicate must not be reused for a call by a different name.
 */
public class FunctionDispatchTest {

    @Test
    public void alternatingNamesInEveryPosition() {

        // token-identical pairs differing only by the function name
        Map<String, Exp<?>> cases = new LinkedHashMap<>();

        cases.put("abs(int(a))", $int("a").abs());
        cases.put("castAsBool(a)", $col("a").castAsBool());

        cases.put("(abs(int(a)))", $int("a").abs());
        cases.put("(castAsBool(a))", $col("a").castAsBool());

        cases.put("shift(abs(int(a)), 1)", $int("a").abs().shift(1));
        cases.put("shift(castAsBool(a), 1)", $col("a").castAsBool().shift(1));

        cases.put("sum(abs(int(a)))", $int("a").abs().sum());
        cases.put("min(abs(int(a)))", $int("a").abs().min());
        cases.put("count(castAsBool(a))", count($col("a").castAsBool()));

        cases.put("abs(int(a)) > 1", $int("a").abs().gt($intVal(1)));
        cases.put("castAsBool(a) = true", $col("a").castAsBool().eq($boolVal(true)));

        cases.put("abs(abs(int(a)))", $int("a").abs().abs());
        cases.put("if(castAsBool(a), abs(int(b)), 0)",
                Exp.ifExp($col("a").castAsBool(), $int("b").abs(), Exp.$val(0)));

        // the first pass populates the DFA, the second one hits it
        for (int pass = 0; pass < 2; pass++) {
            for (Map.Entry<String, Exp<?>> e : cases.entrySet()) {
                assertEquals(e.getValue(), parseExp(e.getKey()), "pass " + pass + ": " + e.getKey());
            }
        }
    }
}
