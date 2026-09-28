package org.dflib.ql;

import org.dflib.Exp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.dflib.Exp.$bool;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$date;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$str;
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
                arguments("plusDays(plusMonths(date(a), 1), 2)", $date("a").plusMonths(1).plusDays(2)),
                arguments("min(plusDays(date(a), 1))", $date("a").plusDays(1).min())
        );
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

    /**
     * An untyped receiver is cast to the type of the only matching overload.
     */
    @ParameterizedTest
    @MethodSource
    public void untypedReceiver_Cast(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> untypedReceiver_Cast() {
        return Stream.of(
                arguments("count(a)", Exp.count($col("a").castAsBool())),
                arguments("if(a, 1, 2)", Exp.ifExp($col("a").castAsBool(), $intVal(1), $intVal(2))),
                arguments("min(str(s), a)", $str("s").min($col("a").castAsBool())),
                arguments("count(first(a))", Exp.count($col("a").first().castAsBool())),
                arguments("if(a, 1, 2) = 1", Exp.ifExp($col("a").castAsBool(), $intVal(1), $intVal(2)).eq($intVal(1)))
        );
    }

    /**
     * An untyped receiver of a function overloaded by receiver type is reported as ambiguous, at the position of the
     * call.
     */
    @ParameterizedTest
    @MethodSource
    public void untypedReceiver_Ambiguous(String text, String message) {
        QLParserException e = assertThrows(QLParserException.class, () -> parseExp(text));
        assertEquals(message, e.getMessage());
    }

    static Stream<Arguments> untypedReceiver_Ambiguous() {
        String year = "Ambiguous call to year(OBJECT): argument 1 is untyped and year is defined for"
                + " [DATE, DATETIME, OFFSETDATETIME] in that position. Cast it explicitly, e.g. year(castAsDate(..))";

        return Stream.of(
                arguments("year(a)", "1:0 " + year),
                arguments("1 +\n  year(a)", "2:2 " + year),
                arguments("plusDays(a, 1)", "1:0 Ambiguous call to plusDays(OBJECT, const NUMERIC): argument 1 is"
                        + " untyped and plusDays is defined for [DATE, DATETIME, OFFSETDATETIME] in that position."
                        + " Cast it explicitly, e.g. plusDays(castAsDate(..))"),
                arguments("max(a)", "1:0 Ambiguous call to max(OBJECT): argument 1 is untyped and max is defined for"
                        + " [STRING, DATE, TIME, DATETIME] in that position. Cast it explicitly, e.g. max(castAsStr(..))")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not shift(bool(a), 1)",
            "min(offsetDateTime(a))",
            "min(x) = min(y)",
    })
    public void unsupportedReceiverOrOperand(String text) {
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
            "if(a, 1, 2)",
            "first(a)",
            "last(a)",
            "vConcat(a, ',')",
    })
    public void toQLRoundTrip(String text) {
        Exp<?> exp = parseExp(text);
        assertEquals(exp, parseExp(exp.toQL()));
    }
}
