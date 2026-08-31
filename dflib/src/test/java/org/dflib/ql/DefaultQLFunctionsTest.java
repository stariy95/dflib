package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
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

    /**
     * The honesty invariant: a descriptor must not promise a return type its producer does not deliver. The parser
     * casts a call result to whatever type the expression rule it was parsed by requires, so a descriptor whose
     * producer returns something else is a ClassCastException inside generated code.
     * <p>
     * Every descriptor is exercised with a single argument list - the one it declares. A built-in that accepts
     * several receiver types declares one {@code call} overload, and so one descriptor, per receiver, so no producer
     * dispatches on its receiver any more and there is no second argument list to try. That the descriptor really is
     * the one these arguments resolve to is {@link #isResolvableByItsOwnSignature}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("defaultDescriptors")
    void returnTypeIsHonest(String label, QLFunctionDescriptor descriptor) {

        List<Exp<?>> args = plausibleArgs(descriptor);
        List<Arg> argTypes = args.stream().map(Arg::of).toList();

        Exp<?> result = descriptor.expProducer().apply(args);
        assertReturnTypeIsHonest(label + " called with " + argTypes, descriptor.returnType(argTypes), result);
    }

    private static void assertReturnTypeIsHonest(String label, TypeClassifier declared, Exp<?> result) {

        TypeClassifier actual = TypeClassifier.classify(result);

        if (declared == TypeClassifier.ANY || declared == TypeClassifier.OBJECT) {
            // An untyped declaration is not a cast site: such a call is only reachable from the untyped
            // "expression" position, which accepts any Exp. ANY and OBJECT are interchangeable there - both mean
            // "no typed rule claims this call" - and which of the two a result classifies as depends on the
            // argument (e.g. "first(date(a))" is ANY while "first(list(a))" is OBJECT). What must not happen is a
            // typed result under an untyped declaration, since a typed rule would then never see it
            assertTrue(actual == TypeClassifier.ANY || actual == TypeClassifier.OBJECT,
                    () -> label + " is declared untyped but produces a " + actual + ": "
                            + result.getClass().getName());
        } else {
            assertEquals(declared, actual,
                    () -> label + " declares a return type its producer does not deliver: "
                            + result.getClass().getName());
        }
    }

    /**
     * Every default descriptor must be reachable through the registry with the very arguments it declares.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("defaultDescriptors")
    void isResolvableByItsOwnSignature(String label, QLFunctionDescriptor descriptor) {

        List<Arg> argTypes = plausibleArgs(descriptor).stream().map(Arg::of).toList();

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

        // 60 names, 159 descriptors. 27 names have exactly one; the rest are overload sets, most of them one
        // overload per receiver type: "year"/"month"/"day" and "hour".."millisecond" have 3 each (21), the 9
        // "plusX" have 3 each (27), "min" and "max" have 5 receivers x {unfiltered, filtered} (20), "avg" and
        // "median" 4 x 2 (16) and "quantile" 4 x 2 (8). "shift" has 8 receivers - the 7 typed ones plus an untyped
        // one - x {no filler, filler} (16). The remaining 24 are arity overloads: "substr", "split", "count",
        // "concat", "sum", "first" and the 4 temporal casts have 2 each; "vConcat" has 4
        assertEquals(159, FUNCTIONS.descriptors().count());
    }

    @Test
    void isFn() {
        assertTrue(FUNCTIONS.isFn("trim"));
        assertTrue(FUNCTIONS.isFn("rowNum"));
        assertTrue(FUNCTIONS.isFn("if"));
        assertFalse(FUNCTIONS.isFn("noSuchFunction"));
    }

    /**
     * The grammar routes a call site by this, so it has to stay true for every name that can produce more than one
     * type. A name with one typed {@code call} overload per receiver qualifies through the second half of the rule -
     * its overloads disagree on their fixed return - rather than by returning the type of an argument.
     */
    @Test
    void isPolymorphicFn() {

        // returns the receiver type: one overload per receiver, each with its own fixed return
        assertTrue(FUNCTIONS.isPolymorphicFn("min"));
        assertTrue(FUNCTIONS.isPolymorphicFn("max"));
        assertTrue(FUNCTIONS.isPolymorphicFn("avg"));
        assertTrue(FUNCTIONS.isPolymorphicFn("median"));
        assertTrue(FUNCTIONS.isPolymorphicFn("quantile"));
        assertTrue(FUNCTIONS.isPolymorphicFn("plusDays"));
        assertTrue(FUNCTIONS.isPolymorphicFn("shift"));

        // fixed return types, single-alternative call sites
        assertFalse(FUNCTIONS.isPolymorphicFn("sum"));
        assertFalse(FUNCTIONS.isPolymorphicFn("cumSum"));
        assertFalse(FUNCTIONS.isPolymorphicFn("count"));
        assertFalse(FUNCTIONS.isPolymorphicFn("year"));
        assertFalse(FUNCTIONS.isPolymorphicFn("castAsDate"));
        assertFalse(FUNCTIONS.isPolymorphicFn("concat"));
        assertFalse(FUNCTIONS.isPolymorphicFn("first"));
        assertFalse(FUNCTIONS.isPolymorphicFn("vConcat"));
    }

    /**
     * An untyped function is only reachable from the untyped expression position: it satisfies no typed rule.
     * <p>
     * ANY and OBJECT are both "untyped" here and are excluded: a name declaring either is not claimed by a typed
     * rule, and the grammar only ever asks {@link QLFunctions#mayReturn} about the seven typed classifiers. Which
     * of the two a declaration uses is an artifact of how it is written - a {@code QLFunction} class returning
     * {@code Exp<?>} classifies as OBJECT, an explicit signature can say ANY.
     */
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

    // Resolution: what the registry picks for a given call

    @Test
    void zeroArityOverloads() {
        assertEquals(0, resolve("count").args().length);
        assertFalse(resolve("count").isVarArgs());

        assertEquals(0, resolve("concat").args().length);
        assertFalse(resolve("concat").isVarArgs());

        assertTrue(resolve("concat", $str("a"), $str("b"), $str("c")).isVarArgs());
        assertTrue(resolve("concat", $str("a")).isVarArgs());

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

    /**
     * An argument whose type is only known at eval time is passable to a typed parameter, so a call by an ANY
     * argument resolves. Whether the producer can then build an expression is a separate question.
     * <p>
     * This holds for a name with a single candidate in that position. A name with several receiver overloads - every
     * one of which such an argument matches equally well - is ambiguous instead, see
     * {@link #untypedReceiverOfAMultiReceiverNameIsAmbiguous()}.
     */
    @Test
    void anyArgumentResolves() {

        // "sum" declares a single numeric receiver, "len" a single string one
        assertSame(resolve("sum", $int("i")), resolve("sum", $col("c")));
        assertSame(resolve("sum", $int("i")), resolve("sum", $date("d").first()));
        assertSame(resolve("len", $str("s")), resolve("len", $col("c")));
    }

    /**
     * A name with several receiver overloads can not be called with an argument whose type is only known at eval
     * time: every overload matches it equally well, so the choice would come down to registration order.
     */
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

        // an expression whose value type is only recoverable at eval time is no better than a bare column ref
        assertThrows(IllegalArgumentException.class, () -> resolve("year", $date("d").first()));
    }

    /**
     * "sum" and "cumSum" are numeric-only and keep a fixed return type, unlike "min"/"max"/"avg"/"median".
     */
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

    /**
     * A polymorphic call's effective return type is the classifier of its receiver. For these names that is not a
     * {@code returningArgType} declaration but a consequence of resolution: the receiver picks the overload, and the
     * overload's own fixed return is the receiver's type.
     */
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

    // Expressions produced: these must be identical to what the grammar builds today

    @Test
    void aggregatesMatchTheUnfilteredApi() {
        // the Exp API's unfiltered overloads delegate to the filtered ones with a null filter, which is what the
        // grammar has always relied on
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

        // an integer literal is narrowed rather than cast
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

        // newly legal: the grammar has no temporal alternative in its "shift" rule
        assertEquals($date("d").shift(1), call("shift", $date("d"), $intVal(1)));
    }

    /**
     * Shifting a Condition produces a plain Exp&lt;Boolean&gt;, since Condition does not override shift(). Its
     * overloads therefore declare a bare {@code Exp<?>} - classifying as OBJECT - rather than BOOLEAN.
     */
    @Test
    void shiftOfAConditionIsUntyped() {
        assertEquals(TypeClassifier.OBJECT, resolve("shift", $bool("b"), $intVal(1)).returnType());
        assertEquals(TypeClassifier.OBJECT,
                resolve("shift", $bool("b"), $intVal(1), $boolVal(true)).returnType());

        assertNotSame(
                resolve("shift", $bool("b"), $intVal(1)),
                resolve("shift", $int("i"), $intVal(1)));
    }

    // Negatives

    @Test
    void plusCountMustBeANumericConstant() {

        // a string count does not match the declared numeric parameter
        assertThrows(IllegalArgumentException.class,
                () -> resolve("plusDays", $date("d"), $strVal("3")));

        // a non-constant count can not be read at parse time
        assertThrows(IllegalArgumentException.class,
                () -> resolve("plusWeeks", $date("d"), $int("n")));
        assertThrows(IllegalArgumentException.class,
                () -> resolve("plusWeeks", $date("d"), $intVal(1).add(2)));
    }

    /**
     * A receiver a function does not support is one it declares no overload for, so the call does not resolve at
     * all. There is no producer-level "unsupported receiver" check left behind these names.
     */
    @Test
    void unsupportedReceiverDoesNotResolve() {

        // OffsetDateTimeExp declares no aggregates
        assertEquals("Function min([OFFSETDATETIME]) not found",
                assertThrows(IllegalArgumentException.class,
                        () -> call("min", $offsetDateTime("odt"))).getMessage());

        // "avg" and "median" are not declared on StrExp
        assertEquals("Function avg([STRING]) not found",
                assertThrows(IllegalArgumentException.class, () -> call("avg", $str("s"))).getMessage());

        // "year" of a time, "hour" of a date
        assertEquals("Function year([TIME]) not found",
                assertThrows(IllegalArgumentException.class, () -> call("year", $time("t"))).getMessage());
        assertEquals("Function hour([DATE]) not found",
                assertThrows(IllegalArgumentException.class, () -> call("hour", $date("d"))).getMessage());

        // "quantile" and "plusX" of an unsupported receiver
        assertThrows(IllegalArgumentException.class, () -> call("quantile", $str("s"), $doubleVal(0.5)));
        assertThrows(IllegalArgumentException.class, () -> call("plusDays", $time("t"), $intVal(1)));
        assertThrows(IllegalArgumentException.class, () -> call("plusHours", $date("d"), $intVal(1)));
    }

    /**
     * The grammar enforced the filler type by having one shift alternative per receiver type, each with a matching
     * scalar filler rule. The untyped overload, which accepts a receiver of any type, has to check it by hand.
     */
    @Test
    void shiftFillerMustMatchTheReceiverType() {

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("shift", $int("i"), $intVal(2), $strVal("replace")));
        assertTrue(e.getMessage().startsWith("shift() filler of type STRING"), e.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> call("shift", $str("s"), $intVal(1), $intVal(1)));
        assertThrows(IllegalArgumentException.class,
                () -> call("shift", $bool("b"), $intVal(1), $strVal("x")));

        // an untyped receiver takes any filler - there is nothing to check against
        assertEquals($col("c").shift(1, "x"), call("shift", $col("c"), $intVal(1), $strVal("x")));
    }

    @Test
    void booleanParameterRejectsAnUntypedArgumentInTheProducer() {
        // "count(a)" resolves via ANY -> BOOLEAN, but an untyped column is not a Condition
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

    // Helpers

    private static QLFunctionDescriptor resolve(String name, Exp<?>... args) {
        return FUNCTIONS.function(name, Arrays.stream(args).map(Arg::of).toList());
    }

    private static Exp<?> call(String name, Exp<?>... args) {
        return resolve(name, args).expProducer().apply(List.of(args));
    }

    private static TypeClassifier effectiveReturnType(String name, Exp<?>... args) {
        List<Arg> argTypes = Arrays.stream(args).map(Arg::of).toList();
        return FUNCTIONS.function(name, argTypes).returnType(argTypes);
    }

    private static String label(QLFunctionDescriptor descriptor) {
        return descriptor.name() + Arrays.toString(descriptor.args())
                + (descriptor.isVarArgs() ? "..." : "")
                + " -> " + (descriptor.returnType() != null
                ? descriptor.returnType()
                : "type of arg " + descriptor.returnArgIndex());
    }

    /**
     * An argument list matching exactly what the descriptor declares: one expression per parameter, of the
     * parameter's own classifier, constant where the parameter is.
     */
    private static List<Exp<?>> plausibleArgs(QLFunctionDescriptor descriptor) {

        List<Exp<?>> args = new ArrayList<>();
        for (Arg a : descriptor.args()) {
            TypeClassifier type = a.type();
            args.add(a.constant() ? constant(type) : column(type));
        }

        if (descriptor.isVarArgs()) {
            // trailing vararg values are unconstrained. Repeat the type of the last declared parameter, so that a
            // function whose varargs are homogeneous with its leading params still gets sensible arguments
            Arg[] declared = descriptor.args();
            TypeClassifier tail = declared.length > 0 ? declared[declared.length - 1].type() : TypeClassifier.STRING;
            args.add(column(tail));
            args.add(column(tail));
        }

        return args;
    }

    /**
     * A non-constant expression of the given type.
     */
    private static Exp<?> column(TypeClassifier type) {
        return switch (type) {
            case NUMERIC -> $int("i");
            case STRING -> $str("s");
            case BOOLEAN -> $bool("b");
            case DATE -> $date("d");
            case TIME -> $time("t");
            case DATETIME -> $dateTime("dt");
            case OFFSETDATETIME -> $offsetDateTime("odt");
            // a genuinely Object-valued expression, as opposed to an untyped column reference
            case OBJECT -> $col("o").list();
            case ANY -> $col("o");
        };
    }

    /**
     * A constant expression of the given type.
     */
    private static Exp<?> constant(TypeClassifier type) {
        return switch (type) {
            case NUMERIC -> $intVal(1);
            case STRING -> $strVal("a");
            case BOOLEAN -> $boolVal(true);
            case DATE -> $dateVal(LocalDate.of(2024, 1, 2));
            case TIME -> $timeVal(LocalTime.of(1, 2, 3));
            case DATETIME -> $dateTimeVal(LocalDateTime.of(2024, 1, 2, 3, 4, 5));
            case OFFSETDATETIME -> $offsetDateTimeVal(OffsetDateTime.parse("2024-01-02T03:04:05+01:00"));
            // there is no constant of an eval-time-only type, so a plain Object constant stands in for it
            case OBJECT, ANY -> $val(new Object());
        };
    }
}
