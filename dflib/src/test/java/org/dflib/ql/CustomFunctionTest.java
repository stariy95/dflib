package org.dflib.ql;

import org.dflib.Environment;
import org.dflib.Exp;
import org.dflib.Udf1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.dflib.Exp.$col;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.parseExp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomFunctionTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void saveFunctions() {
        this.originalFunctions = Environment.commonEnv().getQLFunctions();
    }

    @AfterEach
    public void restoreFunctions() {
        Environment.setQLFunctions(originalFunctions);
    }

    @Test
    public void customFunction_KeepsBuiltIns() {

        Environment.setQLFunctions(QLFunctions.builder()
                .function("twice", new TwiceFunction())
                .build());

        assertEquals(new TwiceFunction().call($int("a").abs()), parseExp("twice(abs(int(a)))"));
        assertEquals($int("a").abs(), parseExp("abs(int(a))"));
        assertEquals($col("a").castAsBool(), parseExp("castAsBool(a)"));
        assertEquals($int("a").sum(), parseExp("sum(int(a))"));
    }

    @Test
    public void builder_SeedsBuiltIns() {
        QLFunctions functions = QLFunctions.builder().build();

        assertTrue(functions.isFn("abs"));
        assertTrue(functions.isFn("trim"));
    }

    @Test
    public void noDefaultFunctions_DropsBuiltIns() {
        QLFunctions functions = QLFunctions.builder()
                .noDefaultFunctions()
                .function("twice", new TwiceFunction())
                .build();

        assertTrue(functions.isFn("twice"));
        assertFalse(functions.isFn("abs"));
    }

    @Test
    public void customFunction_CollidingWithABuiltIn() {

        // reported at build time, as that is when the built-ins are added
        QLFunctions.Builder builder = QLFunctions.builder().function("trim", new TrimFunction());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(e.getMessage().contains("already defined"), e.getMessage());

        assertTrue(QLFunctions.builder()
                .noDefaultFunctions()
                .function("trim", new TrimFunction())
                .build()
                .isFn("trim"));
    }

    private static class TwiceFunction implements Udf1<Integer, Integer> {
        @Override
        public Exp<Integer> call(Exp<Integer> exp) {
            return exp.castAsInt().mul($intVal(2)).castAsInt();
        }
    }

    private static class TrimFunction implements Udf1<Object, String> {
        @Override
        public Exp<String> call(Exp<Object> exp) {
            return exp.castAsStr().trim();
        }
    }
}
