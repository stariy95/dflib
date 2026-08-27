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
 * The grammar routes every function call through a single rule guarded by predicates that read the function name off
 * the token stream. ANTLR shares one DFA cache per decision across all parsers of a JVM, and a prediction made under
 * a name-dependent predicate must not be reused for a different name. This test parses calls of a numeric and of a
 * boolean function alternately, in every syntactic position where a call can appear, and checks that each one keeps
 * resolving to its own function.
 */
public class FunctionDispatchTest {

    @Test
    public void alternatingNamesInEveryPosition() {

        // "abs" is registered as NUMERIC-returning and "castAsBool" as BOOLEAN-returning, so in each pair below the
        // two inputs are token-identical and differ only by the name the predicates see
        Map<String, Exp<?>> cases = new LinkedHashMap<>();

        // a bare call in the untyped expression position
        cases.put("abs(int(a))", $int("a").abs());
        cases.put("castAsBool(a)", $col("a").castAsBool());

        // parenthesized
        cases.put("(abs(int(a)))", $int("a").abs());
        cases.put("(castAsBool(a))", $col("a").castAsBool());

        // as an argument of a special form that dispatches on the argument type
        cases.put("shift(abs(int(a)), 1)", $int("a").abs().shift(1));
        cases.put("shift(castAsBool(a), 1)", $col("a").castAsBool().shift(1));

        // as an argument of an aggregate
        cases.put("sum(abs(int(a)))", $int("a").abs().sum());
        cases.put("min(abs(int(a)))", $int("a").abs().min());
        cases.put("count(castAsBool(a))", count($col("a").castAsBool()));

        // as the left-hand side of a relation
        cases.put("abs(int(a)) > 1", $int("a").abs().gt($intVal(1)));
        cases.put("castAsBool(a) = true", $col("a").castAsBool().eq($boolVal(true)));

        // as a nested argument of another call
        cases.put("abs(abs(int(a)))", $int("a").abs().abs());
        cases.put("if(castAsBool(a), abs(int(b)), 0)",
                Exp.ifExp($col("a").castAsBool(), $int("b").abs(), Exp.$val(0)));

        // Two passes over the same inputs, and within a pass the numeric and the boolean cases interleave. The first
        // pass populates the DFA, the second one hits it
        for (int pass = 0; pass < 2; pass++) {
            for (Map.Entry<String, Exp<?>> e : cases.entrySet()) {
                assertEquals(e.getValue(), parseExp(e.getKey()), "pass " + pass + ": " + e.getKey());
            }
        }
    }
}
