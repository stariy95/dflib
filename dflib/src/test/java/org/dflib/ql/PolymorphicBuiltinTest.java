package org.dflib.ql;

import org.dflib.Exp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.dflib.Exp.$bool;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$date;
import static org.dflib.Exp.$dateTime;
import static org.dflib.Exp.$dateTimeVal;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$str;
import static org.dflib.Exp.$strVal;
import static org.dflib.Exp.$time;
import static org.dflib.Exp.parseExp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Built-in functions whose return type depends on the receiver type.
 */
public class PolymorphicBuiltinTest {

    @ParameterizedTest
    @MethodSource
    public void nestedCallReceiver(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> nestedCallReceiver() {
        return Stream.of(
                arguments("hour(castAsDateTime(x))", $col("x").castAsDateTime().hour()),
                arguments("plusDays(castAsDateTime(x), 1)", $col("x").castAsDateTime().plusDays(1)),
                arguments("min(castAsDate(x))", $col("x").castAsDate().min()),
                arguments("avg(castAsTime(x))", $col("x").castAsTime().avg()),
                arguments("median(castAsDateTime(x))", $col("x").castAsDateTime().median()),
                arguments("min(trim(a))", $col("a").trim().min()),
                arguments("max(lower(a))", $col("a").lower().max()),
                arguments("plusDays(plusMonths(date(a), 1), 2)", $date("a").plusMonths(1).plusDays(2)),
                arguments("min(plusDays(date(a), 1))", $date("a").plusDays(1).min())
        );
    }

    @ParameterizedTest
    @MethodSource
    public void operatorAfterCall(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> operatorAfterCall() {
        return Stream.of(
                arguments("min(int(a)) + 1", $int("a").min().add($intVal(1))),
                arguments("avg(int(a)) * 2 > 5", $int("a").avg().mul($intVal(2)).gt($intVal(5))),
                arguments("- min(int(a))", $int("a").min().negate()),
                arguments("(min(int(a))) + 1", $int("a").min().add($intVal(1))),
                arguments("min(str(a)) = 'x'", $str("a").min().eq($strVal("x"))),
                arguments("avg(int(1)) <= 20", $int(1).avg().le($intVal(20))),
                arguments("min(int(a)) between 1 and 5", $int("a").min().between($intVal(1), $intVal(5))),
                arguments("min(int(a)) in (1, 2)", $int("a").min().in(1, 2))
        );
    }

    @Test
    public void comparison_parameterRhs() {
        LocalDateTime v = LocalDateTime.of(2024, 1, 2, 3, 4);
        assertEquals(
                $dateTime("a").plusDays(1).gt($dateTimeVal(v)),
                parseExp("plusDays(dateTime(a), 1) > ?", v));
    }

    @ParameterizedTest
    @MethodSource
    public void specialForm(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> specialForm() {
        return Stream.of(
                arguments("shift(date(a), 1)", $date("a").shift(1)),
                arguments("shift(time(a), -1)", $time("a").shift(-1)),
                arguments("shift(plusDays(date(a), 1), 1)", $date("a").plusDays(1).shift(1)),

                arguments("first(a, bool(b))", $col("a").first($bool("b"))),
                arguments("last(a)", $col("a").last()),

                arguments("castAsDate(a, 'yyyy')", $col("a").castAsDate("yyyy")),
                arguments("vConcat(a, ',')", $col("a").vConcat(null, ",", "", "")),
                arguments("vConcat(a, bool(b), ',')", $col("a").vConcat($bool("b"), ",", "", "")),
                arguments("vConcat(a, ',', '[', ']')", $col("a").vConcat(null, ",", "[", "]")),
                arguments("vConcat(a, bool(b), ',', '[', ']')", $col("a").vConcat($bool("b"), ",", "[", "]"))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void producerError(String text, String message) {
        QLParserException e = assertThrows(QLParserException.class, () -> parseExp(text));
        assertEquals(message, e.getMessage());
    }

    static Stream<Arguments> producerError() {
        return Stream.of(
                arguments("year(a)", "1:0 Ambiguous call to year(): the type of argument 1 is only known at eval"
                        + " time, and year is defined for [DATE, DATETIME, OFFSETDATETIME] arguments in that"
                        + " position. Cast it, e.g. year(castAsDate(..))"),
                arguments("min(a)", "1:0 Ambiguous call to min(): the type of argument 1 is only known at eval"
                        + " time, and min is defined for [NUMERIC, STRING, DATE, TIME, DATETIME] arguments in that"
                        + " position. Cast it, e.g. min(castAsInt(..))"),
                arguments("plusDays(a, 1)", "1:0 Ambiguous call to plusDays(): the type of argument 1 is only known"
                        + " at eval time, and plusDays is defined for [DATE, DATETIME, OFFSETDATETIME] arguments in"
                        + " that position. Cast it, e.g. plusDays(castAsDate(..))"),
                arguments("avg(str(a))", "1:0 Function avg([STRING]) not found"),
                arguments("plusDays(date(a), 1.5)", "1:0 Not an integer constant: 1.5"),
                arguments("if(a, 1, 2)", "1:0 if() expects argument 1 to be BOOLEAN, got: a")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not shift(bool(a), 1)",
            "min(offsetDateTime(a))",
            "min(x) = min(y)",
    })
    public void throws_(String text) {
        assertThrows(QLParserException.class, () -> parseExp(text));
    }

    // only names whose "toQL" is a call, over receivers that round-trip themselves
    @ParameterizedTest
    @ValueSource(strings = {
            "castAsDate(a)",
            "min(castAsInt(a))",
            "shift(a, 2)",
            "ifNull(a, b)",
            "if(castAsBool(a), 1, 2)",
            "first(a)",
            "last(a)",
            "vConcat(a, ',')",
    })
    public void toQLRoundTrip(String text) {
        Exp<?> exp = parseExp(text);
        assertEquals(exp, parseExp(exp.toQL()));
    }
}
