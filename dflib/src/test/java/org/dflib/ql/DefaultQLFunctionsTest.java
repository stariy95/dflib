package org.dflib.ql;

import org.dflib.Exp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.dflib.Exp.$bool;
import static org.dflib.Exp.$boolVal;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$date;
import static org.dflib.Exp.$dateTime;
import static org.dflib.Exp.$dateTimeVal;
import static org.dflib.Exp.$dateVal;
import static org.dflib.Exp.$decimal;
import static org.dflib.Exp.$doubleVal;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$offsetDateTime;
import static org.dflib.Exp.$offsetDateTimeVal;
import static org.dflib.Exp.$str;
import static org.dflib.Exp.$strVal;
import static org.dflib.Exp.$time;
import static org.dflib.Exp.$timeVal;
import static org.dflib.Exp.$val;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DefaultQLFunctionsTest {

    private static final QLFunctions FUNCTIONS = QLFunctions.builder().build();

    static Stream<Arguments> defaultDescriptors() {
        return FUNCTIONS.descriptors().map(d -> arguments(label(d), d));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("defaultDescriptors")
    void returnTypeIsHonest(String label, QLFunctionDescriptor descriptor) {

        List<Exp<?>> args = plausibleArgs(descriptor);
        List<QLFunctionArg> argTypes = args.stream().map(QLFunctionArg::of).toList();

        Exp<?> result = descriptor.expProducer().apply(args);
        assertReturnTypeIsHonest(label + " called with " + argTypes, descriptor.returnType(), result);
    }

    private static void assertReturnTypeIsHonest(String label, TypeClassifier declared, Exp<?> result) {

        TypeClassifier actual = TypeClassifier.classify(result);

        if (declared == TypeClassifier.ANY || declared == TypeClassifier.OBJECT) {
            // an untyped declaration must not produce a typed result, as a typed rule would never see it
            assertTrue(actual == TypeClassifier.ANY || actual == TypeClassifier.OBJECT,
                    () -> label + " is declared untyped but produces a " + actual + ": "
                            + result.getClass().getName());
        } else {
            assertEquals(declared, actual,
                    () -> label + " declares a return type its producer does not deliver: "
                            + result.getClass().getName());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("defaultDescriptors")
    void isResolvableByItsOwnSignature(String label, QLFunctionDescriptor descriptor) {

        List<QLFunctionArg> argTypes = plausibleArgs(descriptor).stream().map(QLFunctionArg::of).toList();

        assertSame(
                descriptor,
                FUNCTIONS.function(descriptor.name(), argTypes),
                () -> label + " is shadowed by another overload for its own argument types");
    }

    @Test
    void allDefaultsRegistered() {

        List<String> names = FUNCTIONS.descriptors().map(QLFunctionDescriptor::name).distinct().sorted().toList();

        assertEquals(
                List.of(
                        "abs", "avg", "castAsBigint", "castAsBool", "castAsDate", "castAsDateTime", "castAsDecimal",
                        "castAsDouble", "castAsFloat", "castAsInt", "castAsLong", "castAsOffsetDateTime",
                        "castAsStr", "castAsTime", "concat", "contains", "count", "cumSum", "day", "endsWith",
                        "first", "hour", "if", "ifNull", "last", "len", "list", "lower", "matches", "max", "median",
                        "millisecond", "min", "minute", "month", "plusDays", "plusHours", "plusMilliseconds",
                        "plusMinutes", "plusMonths", "plusNanos", "plusSeconds", "plusWeeks", "plusYears",
                        "quantile", "round", "rowNum", "scale", "second", "set", "shift", "split", "sqrt",
                        "startsWith", "substr", "sum", "trim", "upper", "vConcat", "year"),
                names);

        assertEquals(60, names.size());

        // 60 names, 158 descriptors
        assertEquals(158, FUNCTIONS.descriptors().count());
    }

    @Test
    void isFn() {
        assertTrue(FUNCTIONS.isFn("trim"));
        assertTrue(FUNCTIONS.isFn("rowNum"));
        assertTrue(FUNCTIONS.isFn("if"));
        assertFalse(FUNCTIONS.isFn("noSuchFunction"));
    }

    @Test
    void isPolymorphicFn() {
        assertTrue(FUNCTIONS.isPolymorphicFn("min"));
        assertTrue(FUNCTIONS.isPolymorphicFn("max"));
        assertTrue(FUNCTIONS.isPolymorphicFn("avg"));
        assertTrue(FUNCTIONS.isPolymorphicFn("median"));
        assertTrue(FUNCTIONS.isPolymorphicFn("quantile"));
        assertTrue(FUNCTIONS.isPolymorphicFn("plusDays"));
        assertTrue(FUNCTIONS.isPolymorphicFn("shift"));

        assertFalse(FUNCTIONS.isPolymorphicFn("sum"));
        assertFalse(FUNCTIONS.isPolymorphicFn("cumSum"));
        assertFalse(FUNCTIONS.isPolymorphicFn("count"));
        assertFalse(FUNCTIONS.isPolymorphicFn("year"));
        assertFalse(FUNCTIONS.isPolymorphicFn("castAsDate"));
        assertFalse(FUNCTIONS.isPolymorphicFn("concat"));
        assertFalse(FUNCTIONS.isPolymorphicFn("first"));
        assertFalse(FUNCTIONS.isPolymorphicFn("vConcat"));
    }

    @Test
    void untypedFunctionsClaimNoTypedRule() {
        for (String name : List.of("first", "last", "if", "ifNull", "vConcat")) {
            for (TypeClassifier t : TypeClassifier.values()) {
                if (t != TypeClassifier.ANY && t != TypeClassifier.OBJECT) {
                    assertFalse(FUNCTIONS.mayReturn(name, t), name + " may return " + t);
                }
            }
        }
    }

    @Test
    void zeroArityOverloads() {
        assertEquals(0, resolve("count").args().size());
        assertFalse(resolve("count").varArgs());

        assertEquals(0, resolve("concat").args().size());
        assertTrue(resolve("concat").varArgs());

        assertTrue(resolve("concat", $str("a"), $str("b"), $str("c")).varArgs());
        assertTrue(resolve("concat", $str("a")).varArgs());

        assertEquals(Exp.count(), call("count"));
        assertEquals(Exp.concat(), call("concat"));
        assertEquals(Exp.concat($str("a"), $str("b"), $str("c")), call("concat", $str("a"), $str("b"), $str("c")));
    }

    @Test
    void countOfCondition() {
        assertEquals(Exp.count($bool("b")), call("count", $bool("b")));
    }

    @Test
    void allVConcatArities() {
        assertEquals($str("a").vConcat(null, ",", "", ""), call("vConcat", $str("a"), $strVal(",")));
        assertEquals($str("a").vConcat($bool("b"), ",", "", ""),
                call("vConcat", $str("a"), $bool("b"), $strVal(",")));
        assertEquals($str("a").vConcat(null, ",", "[", "]"),
                call("vConcat", $str("a"), $strVal(","), $strVal("["), $strVal("]")));
        assertEquals($str("a").vConcat($bool("b"), ",", "[", "]"),
                call("vConcat", $str("a"), $bool("b"), $strVal(","), $strVal("["), $strVal("]")));
    }

    @Test
    void anyArgumentResolves() {
        assertSame(resolve("sum", $int("i")), resolve("sum", $col("c")));
        assertSame(resolve("sum", $int("i")), resolve("sum", $date("d").first()));
        assertSame(resolve("len", $str("s")), resolve("len", $col("c")));
    }

    @Test
    void untypedReceiverOfAMultiReceiverNameIsAmbiguous() {
        assertEquals("Ambiguous call to year(): the type of argument 1 is only known at eval time, and year is"
                        + " defined for [DATE, DATETIME, OFFSETDATETIME] arguments in that position."
                        + " Cast it, e.g. year(castAsDate(..))",
                assertThrows(IllegalArgumentException.class, () -> resolve("year", $col("c"))).getMessage());

        assertEquals("Ambiguous call to min(): the type of argument 1 is only known at eval time, and min is"
                        + " defined for [NUMERIC, STRING, DATE, TIME, DATETIME] arguments in that position."
                        + " Cast it, e.g. min(castAsInt(..))",
                assertThrows(IllegalArgumentException.class, () -> resolve("min", $col("c"))).getMessage());

        assertThrows(IllegalArgumentException.class, () -> resolve("year", $date("d").first()));
    }

    @Test
    void numericOnlyAggregates() {
        assertEquals(TypeClassifier.NUMERIC, resolve("sum", $int("i")).returnType());
        assertEquals(TypeClassifier.NUMERIC, resolve("sum", $int("i"), $bool("b")).returnType());
        assertEquals(TypeClassifier.NUMERIC, resolve("cumSum", $int("i")).returnType());

        assertEquals($int("i").sum(), call("sum", $int("i")));
        assertEquals($int("i").sum($bool("b")), call("sum", $int("i"), $bool("b")));
        assertEquals($int("i").cumSum(), call("cumSum", $int("i")));

        assertThrows(IllegalArgumentException.class, () -> call("sum", $str("s")));
        assertThrows(IllegalArgumentException.class, () -> call("sum", $date("d")));
    }

    @Test
    void polymorphicReturnFollowsReceiver() {
        assertEquals(TypeClassifier.NUMERIC, effectiveReturnType("min", $int("i")));
        assertEquals(TypeClassifier.STRING, effectiveReturnType("min", $str("s")));
        assertEquals(TypeClassifier.DATE, effectiveReturnType("min", $date("d")));
        assertEquals(TypeClassifier.TIME, effectiveReturnType("min", $time("t")));
        assertEquals(TypeClassifier.DATETIME, effectiveReturnType("min", $dateTime("dt")));

        assertEquals(TypeClassifier.DATE, effectiveReturnType("plusDays", $date("d"), $intVal(1)));
        assertEquals(TypeClassifier.DATETIME, effectiveReturnType("plusDays", $dateTime("dt"), $intVal(1)));
        assertEquals(TypeClassifier.OFFSETDATETIME,
                effectiveReturnType("plusDays", $offsetDateTime("odt"), $intVal(1)));
    }

    @Test
    void aggregatesMatchTheUnfilteredApi() {
        assertEquals($int("i").min(), call("min", $int("i")));
        assertEquals($str("s").max(), call("max", $str("s")));
        assertEquals($date("d").avg(), call("avg", $date("d")));
        assertEquals($time("t").median(), call("median", $time("t")));
        assertEquals($dateTime("dt").min($bool("b")), call("min", $dateTime("dt"), $bool("b")));
        assertEquals($decimal("m").min(), call("min", $decimal("m")));
    }

    @Test
    void quantileReadsADoubleConstant() {
        assertEquals($int("i").quantile(0.5), call("quantile", $int("i"), $doubleVal(0.5)));
        assertEquals($int("i").quantile(0.5, $bool("b")), call("quantile", $int("i"), $doubleVal(0.5), $bool("b")));
        assertEquals($date("d").quantile(0.5), call("quantile", $date("d"), $doubleVal(0.5)));
        assertEquals($int("i").quantile(1.0), call("quantile", $int("i"), $intVal(1)));
    }

    @Test
    void scaleCastsToDecimalFirst() {
        assertEquals($int("i").castAsDecimal().scale(2), call("scale", $int("i"), $intVal(2)));
    }

    @Test
    void fieldFunctions() {
        assertEquals($date("d").year(), call("year", $date("d")));
        assertEquals($date("d").month(), call("month", $date("d")));
        assertEquals($date("d").day(), call("day", $date("d")));
        assertEquals($time("t").hour(), call("hour", $time("t")));
        assertEquals($time("t").millisecond(), call("millisecond", $time("t")));
        assertEquals($dateTime("dt").year(), call("year", $dateTime("dt")));
        assertEquals($dateTime("dt").second(), call("second", $dateTime("dt")));
        assertEquals($offsetDateTime("odt").minute(), call("minute", $offsetDateTime("odt")));
    }

    @Test
    void temporalArithmetic() {
        assertEquals($date("d").plusDays(3), call("plusDays", $date("d"), $intVal(3)));
        assertEquals($dateTime("dt").plusNanos(3), call("plusNanos", $dateTime("dt"), $intVal(3)));
        assertEquals($offsetDateTime("odt").plusWeeks(3), call("plusWeeks", $offsetDateTime("odt"), $intVal(3)));
        assertEquals($time("t").plusMilliseconds(3), call("plusMilliseconds", $time("t"), $intVal(3)));
    }

    @Test
    void casts() {
        assertEquals($col("c").castAsInt(), call("castAsInt", $col("c")));
        assertEquals($col("c").castAsDecimal(), call("castAsDecimal", $col("c")));
        assertEquals($col("c").castAsStr(), call("castAsStr", $col("c")));
        assertEquals($col("c").castAsDate(), call("castAsDate", $col("c")));
        assertEquals($col("c").castAsDate("yyyy"), call("castAsDate", $col("c"), $strVal("yyyy")));
        assertEquals($col("c").castAsOffsetDateTime("yyyy"),
                call("castAsOffsetDateTime", $col("c"), $strVal("yyyy")));
    }

    @Test
    void specialForms() {
        assertEquals(Exp.ifExp($bool("b"), $int("i"), $int("j")), call("if", $bool("b"), $int("i"), $int("j")));
        assertEquals(Exp.ifNull($int("i"), $int("j")), call("ifNull", $int("i"), $int("j")));
        assertEquals($col("c").first(), call("first", $col("c")));
        assertEquals($col("c").first($bool("b")), call("first", $col("c"), $bool("b")));
        assertEquals($col("c").last(), call("last", $col("c")));
    }

    @Test
    void shiftProducesTheSameExpressionsAsTheGrammar() {
        assertEquals($int("i").shift(2), call("shift", $int("i"), $intVal(2)));
        assertEquals($int("i").shift(2, 0), call("shift", $int("i"), $intVal(2), $intVal(0)));
        assertEquals($str("s").shift(1, "x"), call("shift", $str("s"), $intVal(1), $strVal("x")));
        assertEquals($bool("b").shift(1, true), call("shift", $bool("b"), $intVal(1), $boolVal(true)));
        assertEquals($date("d").shift(1), call("shift", $date("d"), $intVal(1)));
    }

    @Test
    void shiftOfAConditionIsUntyped() {
        assertEquals(TypeClassifier.OBJECT, resolve("shift", $bool("b"), $intVal(1)).returnType());
        assertEquals(TypeClassifier.OBJECT,
                resolve("shift", $bool("b"), $intVal(1), $boolVal(true)).returnType());

        assertNotSame(
                resolve("shift", $bool("b"), $intVal(1)),
                resolve("shift", $int("i"), $intVal(1)));
    }

    @Test
    void plusCountMustBeANumericConstant() {
        assertThrows(IllegalArgumentException.class,
                () -> resolve("plusDays", $date("d"), $strVal("3")));
        assertThrows(IllegalArgumentException.class,
                () -> resolve("plusWeeks", $date("d"), $int("n")));
        assertThrows(IllegalArgumentException.class,
                () -> resolve("plusWeeks", $date("d"), $intVal(1).add(2)));
    }

    @Test
    void unsupportedReceiverDoesNotResolve() {
        assertEquals("Function min([OFFSETDATETIME]) not found",
                assertThrows(IllegalArgumentException.class,
                        () -> call("min", $offsetDateTime("odt"))).getMessage());
        assertEquals("Function avg([STRING]) not found",
                assertThrows(IllegalArgumentException.class, () -> call("avg", $str("s"))).getMessage());
        assertEquals("Function year([TIME]) not found",
                assertThrows(IllegalArgumentException.class, () -> call("year", $time("t"))).getMessage());
        assertEquals("Function hour([DATE]) not found",
                assertThrows(IllegalArgumentException.class, () -> call("hour", $date("d"))).getMessage());

        assertThrows(IllegalArgumentException.class, () -> call("quantile", $str("s"), $doubleVal(0.5)));
        assertThrows(IllegalArgumentException.class, () -> call("plusDays", $time("t"), $intVal(1)));
        assertThrows(IllegalArgumentException.class, () -> call("plusHours", $date("d"), $intVal(1)));
    }

    @Test
    void shiftFillerMustMatchTheReceiverType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("shift", $int("i"), $intVal(2), $strVal("replace")));
        assertTrue(e.getMessage().startsWith("shift() filler of type STRING"), e.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> call("shift", $str("s"), $intVal(1), $intVal(1)));
        assertThrows(IllegalArgumentException.class,
                () -> call("shift", $bool("b"), $intVal(1), $strVal("x")));

        assertEquals($col("c").shift(1, "x"), call("shift", $col("c"), $intVal(1), $strVal("x")));
    }

    @Test
    void booleanParameterRejectsAnUntypedArgumentInTheProducer() {
        assertEquals("count() expects argument 1 to be BOOLEAN, got: a",
                assertThrows(IllegalArgumentException.class, () -> call("count", $col("a"))).getMessage());
    }

    @Test
    void unknownFunctionsAndArities() {
        assertThrows(IllegalArgumentException.class, () -> resolve("noSuchFunction", $int("i")));
        assertThrows(IllegalArgumentException.class, () -> resolve("rowNum", $int("i")));
        assertThrows(IllegalArgumentException.class, () -> resolve("year"));
        assertThrows(IllegalArgumentException.class, () -> resolve("last", $col("c"), $bool("b")));
    }

    private static QLFunctionDescriptor resolve(String name, Exp<?>... args) {
        return FUNCTIONS.function(name, Arrays.stream(args).map(QLFunctionArg::of).toList());
    }

    private static Exp<?> call(String name, Exp<?>... args) {
        return resolve(name, args).expProducer().apply(List.of(args));
    }

    private static TypeClassifier effectiveReturnType(String name, Exp<?>... args) {
        return resolve(name, args).returnType();
    }

    private static String label(QLFunctionDescriptor descriptor) {
        return descriptor.name() + descriptor.args().toString()
                + (descriptor.varArgs() ? "..." : "")
                + " -> " + descriptor.returnType();
    }

    /**
     * An argument list matching exactly what the descriptor declares.
     */
    private static List<Exp<?>> plausibleArgs(QLFunctionDescriptor descriptor) {

        List<Exp<?>> args = new ArrayList<>();
        for (QLFunctionArg a : descriptor.args()) {
            TypeClassifier type = a.type();
            args.add(a.constant() ? constant(type) : column(type));
        }

        if (descriptor.varArgs()) {
            List<QLFunctionArg> declared = descriptor.args();
            TypeClassifier tail = declared.isEmpty() ? TypeClassifier.STRING : declared.getLast().type();
            args.add(column(tail));
            args.add(column(tail));
        }

        return args;
    }

    private static Exp<?> column(TypeClassifier type) {
        return switch (type) {
            case NUMERIC -> $int("i");
            case STRING -> $str("s");
            case BOOLEAN -> $bool("b");
            case DATE -> $date("d");
            case TIME -> $time("t");
            case DATETIME -> $dateTime("dt");
            case OFFSETDATETIME -> $offsetDateTime("odt");
            case OBJECT -> $col("o").list();
            case ANY -> $col("o");
        };
    }

    private static Exp<?> constant(TypeClassifier type) {
        return switch (type) {
            case NUMERIC -> $intVal(1);
            case STRING -> $strVal("a");
            case BOOLEAN -> $boolVal(true);
            case DATE -> $dateVal(LocalDate.of(2024, 1, 2));
            case TIME -> $timeVal(LocalTime.of(1, 2, 3));
            case DATETIME -> $dateTimeVal(LocalDateTime.of(2024, 1, 2, 3, 4, 5));
            case OFFSETDATETIME -> $offsetDateTimeVal(OffsetDateTime.parse("2024-01-02T03:04:05+01:00"));
            case OBJECT, ANY -> $val(new Object());
        };
    }
}
