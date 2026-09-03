package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DecimalExp;
import org.dflib.Environment;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.StrExp;
import org.dflib.Udf1;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
import org.dflib.ql.fn.OtherPackageFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.dflib.Exp.$bool;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$date;
import static org.dflib.Exp.$doubleVal;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$str;
import static org.dflib.Exp.$strVal;
import static org.dflib.Exp.parseExp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reflective registration of a {@link QLFunction}.
 */
public class QLFunctionReflectionTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void saveFunctions() {
        this.originalFunctions = Environment.commonEnv().getQLFunctions();
    }

    @AfterEach
    public void restoreFunctions() {
        Environment.setQLFunctions(originalFunctions);
    }

    private static QLFunctions registry(String name, QLFunction function) {
        return QLFunctions.builder().noDefaultFunctions().function(name, function).build();
    }

    private static Exp<?> call(QLFunctions functions, String name, Exp<?>... args) {
        List<Exp<?>> exps = Arrays.asList(args);
        List<Arg> descriptors = exps.stream().map(Arg::of).toList();
        return functions.function(name, descriptors).expProducer().apply(exps);
    }

    private static List<String> shapes(QLFunctions functions) {
        return functions.descriptors()
                .map(d -> d.returnType() + " " + Arrays.toString(d.args()) + (d.isVarArgs() ? "..." : ""))
                .toList();
    }

    @Test
    public void overloads_ResolveByArgumentType() {

        QLFunctions functions = registry("min", new MinFunction());

        assertEquals(3, functions.descriptors().count());

        assertEquals($int("a").min(), call(functions, "min", $int("a")));
        assertEquals($str("a").min(), call(functions, "min", $str("a")));
        assertEquals($date("a").min(), call(functions, "min", $date("a")));

        assertTrue(functions.mayReturn("min", TypeClassifier.NUMERIC));
        assertTrue(functions.mayReturn("min", TypeClassifier.STRING));
        assertTrue(functions.mayReturn("min", TypeClassifier.DATE));
        assertFalse(functions.mayReturn("min", TypeClassifier.TIME));
        assertFalse(functions.mayReturn("min", TypeClassifier.OBJECT));
        assertTrue(functions.isPolymorphicFn("min"));
    }

    @Test
    public void intParameter_IsAConstantArg() {

        QLFunctions functions = registry("plusDays", new PlusDaysFunction());

        QLFunctionDescriptor d = functions.function("plusDays",
                List.of(new Arg(TypeClassifier.DATE, false), new Arg(TypeClassifier.NUMERIC, true)));
        assertEquals("[DATE, const NUMERIC]", Arrays.toString(d.args()));

        assertEquals($date("a").plusDays(3), call(functions, "plusDays", $date("a"), $intVal(3)));
    }

    @Test
    public void intParameter_RejectsAFraction() {

        QLFunctions functions = registry("plusDays", new PlusDaysFunction());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call(functions, "plusDays", $date("a"), $doubleVal(1.5)));
        assertEquals("Not an integer constant: 1.5", e.getMessage());
    }

    @Test
    public void constantParameter_RejectsANonConstantArg() {

        QLFunctions functions = registry("plusDays", new PlusDaysFunction());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call(functions, "plusDays", $date("a"), $int("b")));
        assertEquals("Function plusDays([DATE, NUMERIC]) not found", e.getMessage());
    }

    @Test
    public void doubleParameter_IsAConstantArg() {

        QLFunctions functions = registry("quantile", new QuantileFunction());

        assertEquals($int("a").quantile(0.5), call(functions, "quantile", $int("a"), $doubleVal(0.5)));
        assertEquals($int("a").quantile(1.0), call(functions, "quantile", $int("a"), $intVal(1)));
    }

    @Test
    public void stringParameter_IsAConstantArg() {

        QLFunctions functions = registry("castAsDate", new CastAsDateFunction());

        assertEquals($col("a").castAsDate("yyyy-MM-dd"),
                call(functions, "castAsDate", $col("a"), $strVal("yyyy-MM-dd")));
    }

    @Test
    public void numberBoundedTypeVariable_IsANumericConstantArg() {

        QLFunctions functions = registry("shift", new ShiftFunction());

        assertEquals("[NUMERIC, const NUMERIC, const NUMERIC]",
                Arrays.toString(functions.descriptors().findFirst().orElseThrow().args()));

        assertEquals($int("a").shift(2, 0), call(functions, "shift", $int("a"), $intVal(2), $intVal(0)));
    }

    @Test
    public void genericExpReturn_IsObject() {

        QLFunctions functions = registry("first", new FirstFunction());

        assertEquals(TypeClassifier.OBJECT, functions.descriptors().findFirst().orElseThrow().returnType());
        assertTrue(functions.mayReturn("first", TypeClassifier.OBJECT));

        for (TypeClassifier t : List.of(TypeClassifier.NUMERIC, TypeClassifier.STRING, TypeClassifier.BOOLEAN,
                TypeClassifier.DATE, TypeClassifier.TIME, TypeClassifier.DATETIME, TypeClassifier.OFFSETDATETIME)) {
            assertFalse(functions.mayReturn("first", t), t.name());
        }

        assertFalse(functions.hasTypedReturn("first"));
        assertEquals($col("a").first(), call(functions, "first", $col("a")));
    }

    @Test
    public void varArgs() {

        QLFunctions functions = registry("concat", new ConcatFunction());

        assertEquals(2, functions.descriptors().count());

        assertEquals(Exp.concat(), call(functions, "concat"));

        assertEquals(Exp.concat($str("a")), call(functions, "concat", $str("a")));
        assertEquals(Exp.concat($str("a"), $col("b"), $intVal(1)),
                call(functions, "concat", $str("a"), $col("b"), $intVal(1)));
    }

    @Test
    public void typedParameter_RejectsAnUntypedArgAtProduceTime() {

        QLFunctions functions = registry("not", new NotFunction());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call(functions, "not", $col("a")));
        assertEquals("not() expects argument 1 to be BOOLEAN, got: " + $col("a").toQL(), e.getMessage());
    }

    @Test
    public void bareTypedExpReturn_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("bad", new BareExpReturnFunction()));
        assertTrue(e.getMessage().contains("would claim to produce STRING while producing a bare Exp"),
                e.getMessage());
        assertTrue(e.getMessage().contains("Declare the Exp subinterface actually produced (e.g. StrExp), or Exp<?>"),
                e.getMessage());
    }

    @Test
    public void nonExpReturn_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("bad", new NonExpReturnFunction()));
        assertTrue(e.getMessage().contains("must return an Exp"), e.getMessage());
    }

    @Test
    public void unsupportedConstantType_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("bad", new UnsupportedConstantFunction()));
        assertTrue(e.getMessage().contains("must be declared as one of"), e.getMessage());
    }

    @Test
    public void alsoAUdf_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("bad", (QLFunction) new AlsoAUdfFunction()));
        assertTrue(e.getMessage().contains("must not also implement Udf0..UdfN"), e.getMessage());
    }

    @Test
    public void nonPublicClass_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("bad", new NonPublicFunction()));
        assertTrue(e.getMessage().contains("must be public to be invoked reflectively"), e.getMessage());
    }

    @Test
    public void noCallMethod_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("bad", new NoCallFunction()));
        assertTrue(e.getMessage().contains("must declare at least one public 'call' method"), e.getMessage());
    }

    @Test
    public void sameArgShapeOverloads_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("dup", new SameShapeFunction()));
        assertEquals("Function dup([NUMERIC]) already defined", e.getMessage());
    }

    @Test
    public void overloadOrder_IsDeterministic() {
        assertEquals(
                List.of("NUMERIC [NUMERIC]", "STRING [STRING]", "DATE [DATE]"),
                shapes(registry("min", new MinFunction())));

        assertEquals(
                List.of("NUMERIC []",
                        "NUMERIC [NUMERIC]",
                        "STRING [STRING]",
                        "NUMERIC [NUMERIC, const NUMERIC]"),
                shapes(registry("mixed", new MixedArityFunction())));

        assertEquals(shapes(registry("mixed", new MixedArityFunction())),
                shapes(registry("mixed", new MixedArityFunction())));
    }

    @Test
    public void bodyException_IsUnwrappedAndPositioned() {

        Environment.setQLFunctions(QLFunctions.builder().function("boom", new BoomFunction()).build());

        QLParserException e = assertThrows(QLParserException.class, () -> parseExp("1 + boom(int(a))"));
        assertEquals("1:4 boom() does not like " + $int("a").toQL(), e.getMessage());
    }

    @Test
    public void publicClassInAnotherPackage_IsInvokable() {

        QLFunctions functions = registry("other", new OtherPackageFunction());

        assertEquals(3, functions.descriptors().count());

        assertEquals($col("a").castAsStr().len(), call(functions, "other", $col("a")));
        assertEquals($int("a").shift(2), call(functions, "other", $int("a"), $intVal(2)));
        assertEquals(Exp.concat($str("a"), $str("b"), $str("c")),
                call(functions, "other", $str("a"), $str("b"), $str("c")));
    }

    public static class MinFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) {
            return e.min();
        }

        public StrExp call(StrExp e) {
            return e.min();
        }

        public DateExp call(DateExp e) {
            return e.min();
        }
    }

    public static class MixedArityFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e, int n) {
            return e.shift(n);
        }

        public StrExp call(StrExp e) {
            return e.trim();
        }

        public NumExp<?> call() {
            return Exp.count();
        }

        public NumExp<?> call(NumExp<?> e) {
            return e.abs();
        }
    }

    public static class PlusDaysFunction implements QLFunction {

        public DateExp call(DateExp e, int days) {
            return e.plusDays(days);
        }
    }

    public static class QuantileFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e, double q) {
            return e.quantile(q);
        }
    }

    public static class CastAsDateFunction implements QLFunction {

        public DateExp call(Exp<?> e, String format) {
            return e.castAsDate(format);
        }
    }

    public static class ShiftFunction implements QLFunction {

        public <N extends Number> NumExp<N> call(NumExp<N> e, int offset, N filler) {
            return e.shift(offset, filler);
        }
    }

    public static class FirstFunction implements QLFunction {

        public <T> Exp<T> call(Exp<T> e) {
            return e.first();
        }
    }

    public static class ConcatFunction implements QLFunction {

        public StrExp call() {
            return Exp.concat();
        }

        public StrExp call(Exp<?>... exps) {
            return Exp.concat((Object[]) exps);
        }
    }

    public static class NotFunction implements QLFunction {

        public Condition call(Condition c) {
            return c.not();
        }
    }

    public static class BoomFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) {
            throw new IllegalArgumentException("boom() does not like " + e.toQL());
        }
    }

    public static class BareExpReturnFunction implements QLFunction {

        public Exp<String> call(Exp<?> e) {
            return e.castAsStr();
        }
    }

    public static class NonExpReturnFunction implements QLFunction {

        public String call(Exp<?> e) {
            return e.toQL();
        }
    }

    public static class UnsupportedConstantFunction implements QLFunction {

        public StrExp call(StrExp e, StringBuilder unsupported) {
            return e.trim();
        }
    }

    public static class AlsoAUdfFunction implements QLFunction, Udf1<Object, Object> {

        @Override
        public Exp<Object> call(Exp<Object> exp) {
            return exp;
        }
    }

    static class NonPublicFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) {
            return e.abs();
        }
    }

    public static class NoCallFunction implements QLFunction {

        public NumExp<?> notCall(NumExp<?> e) {
            return e.abs();
        }
    }

    public static class SameShapeFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) {
            return e.abs();
        }

        public DecimalExp call(DecimalExp e) {
            return e;
        }
    }
}
