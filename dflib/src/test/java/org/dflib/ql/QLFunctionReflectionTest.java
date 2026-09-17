package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DecimalExp;
import org.dflib.Environment;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.StrExp;
import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.UdfN;
import org.dflib.ql.fn.OtherPackageFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.dflib.Exp.$bool;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$date;
import static org.dflib.Exp.$decimal;
import static org.dflib.Exp.$doubleVal;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$str;
import static org.dflib.Exp.$strVal;
import static org.dflib.Exp.parseExp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reflective registration of {@link QLFunction} classes and {@code Udf} objects.
 */
public class QLFunctionReflectionTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void saveFunctions() {
        this.originalFunctions = Environment.commonEnv().qlFunctions();
    }

    @AfterEach
    public void restoreFunctions() {
        Environment.setQLFunctions(originalFunctions);
    }

    private static QLFunctions registry(String name, QLFunction function) {
        return QLFunctions.builder().noDefaultFunctions().function(name, function).build();
    }

    private static Exp<?> call(QLFunctions functions, String name, Exp<?>... args) {
        return functions.call(name, Arrays.asList(args));
    }

    private static List<String> shapes(QLFunctions functions) {
        return functions.descriptors().map(QLFunctionDescriptor::shape).toList();
    }

    @Test
    public void overloads_ResolveByArgumentType() {

        QLFunctions functions = registry("min", new MinFunction());

        assertEquals(3, functions.descriptors().count());

        assertEquals($int("a").min(), call(functions, "min", $int("a")));
        assertEquals($str("a").min(), call(functions, "min", $str("a")));
        assertEquals($date("a").min(), call(functions, "min", $date("a")));
    }

    @Test
    public void intParameter_IsAConstantArg() {

        QLFunctions functions = registry("plusDays", new PlusDaysFunction());

        assertEquals(List.of("plusDays(DATE, const NUMERIC)"), shapes(functions));
        assertEquals($date("a").plusDays(3), call(functions, "plusDays", $date("a"), $intVal(3)));
        assertEquals($date("a").plusDays(3), call(functions, "plusDays", $date("a"), Exp.$longVal(3L)));
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
        assertEquals("No overload of plusDays matches plusDays(DATE, NUMERIC). Available: plusDays(DATE, const NUMERIC)",
                e.getMessage());
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
    public void objectParameter_IsAConstantArgOfAnyType() {

        QLFunctions functions = registry("shift", new ObjectFillerFunction());

        assertEquals(List.of("shift(OBJECT, const NUMERIC, const OBJECT)"), shapes(functions));

        assertEquals($col("a").shift(1, "x"), call(functions, "shift", $col("a"), $intVal(1), $strVal("x")));
        assertEquals($col("a").shift(1, 5), call(functions, "shift", $col("a"), $intVal(1), $intVal(5)));
        assertEquals($col("a").shift(1, null), call(functions, "shift", $col("a"), $intVal(1), Exp.$val(null)));

        assertThrows(IllegalArgumentException.class,
                () -> call(functions, "shift", $col("a"), $intVal(1), $col("b")));
    }

    @Test
    public void numberBoundedTypeVariable_IsANumericConstantArg() {

        QLFunctions functions = registry("shift", new ShiftFunction());

        assertEquals(List.of("shift(NUMERIC, const NUMERIC, const NUMERIC)"), shapes(functions));
        assertEquals($int("a").shift(2, 0), call(functions, "shift", $int("a"), $intVal(2), $intVal(0)));
    }

    @Test
    public void expOfATypeVariable_AcceptsAnything() {

        QLFunctions functions = registry("first", new FirstFunction());

        assertEquals(List.of("first(OBJECT)"), shapes(functions));
        assertEquals($col("a").first(), call(functions, "first", $col("a")));
        assertEquals($int("a").first(), call(functions, "first", $int("a")));
    }

    @Test
    public void varArgs() {

        QLFunctions functions = registry("concat", new ConcatFunction());

        assertEquals(List.of("concat()", "concat(...)"), shapes(functions));

        assertEquals(Exp.concat(), call(functions, "concat"));
        assertEquals(Exp.concat($str("a")), call(functions, "concat", $str("a")));
        assertEquals(Exp.concat($str("a"), $col("b"), $intVal(1)),
                call(functions, "concat", $str("a"), $col("b"), $intVal(1)));
    }

    @Test
    public void varArgs_TypedComponent_Rejected() {

        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.function("bad", new TypedVarArgsFunction()));
        assertTrue(e.getMessage().contains("must be an Exp<?> array"), e.getMessage());
    }

    @Test
    public void typedParameter_RejectsAnUntypedArg() {

        QLFunctions functions = registry("not", new NotFunction());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call(functions, "not", $col("a")));
        assertEquals("No overload of not matches not(OBJECT). Available: not(BOOLEAN)."
                + " Argument 1 is untyped; cast it to one of [BOOLEAN], e.g. castAsBool(..)", e.getMessage());
    }

    @Test
    public void narrowExpParameter_RejectsAWiderArgAtProduceTime() {

        // "DecimalExp" classifies as NUMERIC, so the resolver accepts any NumExp, and the producer has to check
        QLFunctions functions = registry("dec", new DecimalFunction());

        assertEquals($decimal("a").abs(), call(functions, "dec", $decimal("a")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call(functions, "dec", $int("a")));
        assertEquals("dec() expects argument 1 to be a DecimalExp, got: a", e.getMessage());
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
        assertEquals("Function dup(NUMERIC) already defined", e.getMessage());
    }

    @Test
    public void overloadOrder_IsDeterministic() {
        assertEquals(
                List.of("min(NUMERIC)", "min(STRING)", "min(DATE)"),
                shapes(registry("min", new MinFunction())));

        assertEquals(
                List.of("mixed()", "mixed(NUMERIC)", "mixed(STRING)", "mixed(NUMERIC, const NUMERIC)"),
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
    public void bodyException_AnyRuntimeExceptionIsPositioned() {

        Environment.setQLFunctions(QLFunctions.builder()
                .function("state", new StateFunction())
                .function("npe", new NpeFunction())
                .function("regex", new RegexFunction())
                .build());

        assertEquals("1:4 not ready",
                assertThrows(QLParserException.class, () -> parseExp("1 + state(int(a))")).getMessage());

        // no message: the exception class stands in for it
        assertEquals("2:0 NullPointerException",
                assertThrows(QLParserException.class, () -> parseExp("1 +\nnpe(int(a))")).getMessage());

        // a library exception from the body
        assertTrue(assertThrows(QLParserException.class, () -> parseExp("regex(str(a), '[')")).getMessage()
                .startsWith("1:0 Unclosed character class"));
    }

    @Test
    public void bodyException_CheckedExceptionIsWrappedAndPositioned() {

        Environment.setQLFunctions(QLFunctions.builder().function("io", new CheckedFunction()).build());

        assertEquals("1:4 Error calling io(): disk is full",
                assertThrows(QLParserException.class, () -> parseExp("1 + io(int(a))")).getMessage());
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

    // Udf objects

    @Test
    public void udf_TypedByItsGenericSignature() {

        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f0", new CountUdf())
                .function("f1", new IntUdf())
                .function("f2", new IntStrUdf())
                .function("fN", new VarArgsUdf())
                .build();

        assertEquals(List.of("f0()", "f1(NUMERIC)", "f2(NUMERIC, STRING)", "fN(...)"), shapes(functions));

        assertEquals(Exp.count(), call(functions, "f0"));
        assertEquals($int("a").castAsInt(), call(functions, "f1", $int("a")));
        assertEquals(Exp.concat($int("a"), $str("b")), call(functions, "f2", $int("a"), $str("b")));
        assertEquals($int("a").add($int("b")), call(functions, "fN", $int("a"), $int("b")));

        assertThrows(IllegalArgumentException.class, () -> call(functions, "f1", $str("a")));
        assertThrows(IllegalArgumentException.class, () -> call(functions, "f1", $col("a")));
    }

    @Test
    public void udf_CovariantReturn() {

        // the bridge "call" emitted for a covariant return carries no generic types and must be skipped
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions().function("scale", new CovariantUdf()).build();

        assertEquals(List.of("scale(NUMERIC, NUMERIC)"), shapes(functions));
    }

    @Test
    public void udf_Lambda_IsUntyped() {

        // a lambda's "call" is erased to "Exp call(Exp)", so it accepts arguments of any type
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f0", Udf0.of(Exp::count))
                .function("f1", Udf1.of(e -> e.castAsStr().trim()))
                .function("f2", Udf2.of((a, b) -> Exp.concat(a, b)))
                .function("fN", UdfN.of(Exp::concat))
                .build();

        assertEquals(List.of("f0()", "f1(OBJECT)", "f2(OBJECT, OBJECT)", "fN(...)"), shapes(functions));

        assertEquals($col("a").castAsStr().trim(), call(functions, "f1", $col("a")));
        assertEquals($int("a").castAsStr().trim(), call(functions, "f1", $int("a")));
        assertEquals(Exp.concat($int("a"), $str("b")), call(functions, "f2", $int("a"), $str("b")));
        assertEquals(Exp.concat($int("a"), $str("b"), $col("c")), call(functions, "fN", $int("a"), $str("b"), $col("c")));
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

    public static class ObjectFillerFunction implements QLFunction {

        @SuppressWarnings("unchecked")
        public <T> Exp<T> call(Exp<T> e, int offset, Object filler) {
            return e.shift(offset, (T) filler);
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

    public static class TypedVarArgsFunction implements QLFunction {

        public StrExp call(StrExp... exps) {
            return Exp.concat((Object[]) exps);
        }
    }

    public static class NotFunction implements QLFunction {

        public Condition call(Condition c) {
            return c.not();
        }
    }

    public static class DecimalFunction implements QLFunction {

        public NumExp<?> call(DecimalExp e) {
            return e.abs();
        }
    }

    public static class BoomFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) {
            throw new IllegalArgumentException("boom() does not like " + e.toQL());
        }
    }

    public static class StateFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) {
            throw new IllegalStateException("not ready");
        }
    }

    public static class NpeFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) {
            throw new NullPointerException();
        }
    }

    public static class RegexFunction implements QLFunction {

        public Condition call(StrExp e, String regex) {
            return e.matches(regex);
        }
    }

    public static class CheckedFunction implements QLFunction {

        public NumExp<?> call(NumExp<?> e) throws java.io.IOException {
            throw new java.io.IOException("disk is full");
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

    public static class CountUdf implements Udf0<Integer> {
        @Override
        public Exp<Integer> call() {
            return Exp.count();
        }
    }

    public static class IntUdf implements Udf1<Integer, Integer> {
        @Override
        public Exp<Integer> call(Exp<Integer> exp) {
            return exp.castAsInt();
        }
    }

    public static class IntStrUdf implements Udf2<Integer, String, String> {
        @Override
        public Exp<String> call(Exp<Integer> a, Exp<String> b) {
            return Exp.concat(a, b);
        }
    }

    public static class VarArgsUdf implements UdfN<Number> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public NumExp<Number> call(Exp<?>... exps) {
            NumExp<Number> result = (NumExp) exps[0];
            for (int i = 1; i < exps.length; i++) {
                result = result.add((NumExp) exps[i]);
            }
            return result;
        }
    }

    public static class CovariantUdf implements Udf2<Number, Integer, Number> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public NumExp<Number> call(Exp<Number> exp, Exp<Integer> scale) {
            return (NumExp) exp.castAsDecimal();
        }
    }
}
