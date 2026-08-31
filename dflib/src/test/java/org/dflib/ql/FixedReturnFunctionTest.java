package org.dflib.ql;

import org.dflib.Exp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.dflib.Exp.$col;
import static org.dflib.Exp.$dateTimeVal;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$strVal;
import static org.dflib.Exp.count;
import static org.dflib.Exp.parseExp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Pins the behavior of the functions whose return type is fixed by their name and which are resolved by the
 * {@link QLFunctions} registry rather than by a dedicated grammar rule: the numeric casts, the temporal field
 * accessors, {@code count}, {@code scale}, {@code sum}, {@code cumSum} and {@code concat}.
 * <p>
 * The per-name grammar rules these replace typed their argument, so a call was routed by the syntax of its argument.
 * The registry declares a single receiver instead and dispatches on the expression the argument actually produced,
 * which is what makes {@code year(?)} bound to a {@code LocalDateTime} resolve correctly.
 */
public class FixedReturnFunctionTest {

    @Test
    public void fieldFn_boundParameter() {

        // the four "*FieldFn" grammar rules each declared their own receiver type, and a bound parameter is not one
        // of them until it has been evaluated: "year(?)" was matched by "dateFieldFn", so a LocalDateTime parameter
        // was rejected as a date, and "hour(?)" by "timeFieldFn", which made it a time-of-day of the wrong value.
        // The registry passes the parameter through as an untyped expression and dispatches on what it produced
        LocalDateTime ldt = LocalDateTime.of(2020, 1, 2, 3, 4, 5);

        assertEquals($dateTimeVal(ldt).year(), parseExp("year(?)", ldt));
        assertEquals($dateTimeVal(ldt).hour(), parseExp("hour(?)", ldt));
    }

    /**
     * A null literal is an Object-valued constant, and no field function declares an OBJECT receiver, so the call
     * does not resolve.
     */
    @ParameterizedTest
    @ValueSource(strings = {"year(null)", "hour(null)"})
    public void fieldFn_null_throws(String text) {
        QLParserException e = assertThrows(QLParserException.class, () -> parseExp(text));
        assertEquals("1:0 Function " + text.substring(0, text.indexOf('('))
                + "([const OBJECT]) not found", e.getMessage());
    }

    @Test
    public void fieldFn_nullParameter_throws() {
        QLParserException e = assertThrows(QLParserException.class,
                () -> parseExp("month(?)", new Object[]{null}));
        assertEquals("1:0 Function month([const OBJECT]) not found", e.getMessage());
    }

    @Test
    public void count_untypedColumn() {

        // "count(a)" used to be a syntax error: the grammar rule demanded a boolean expression, and an untyped
        // column reference is not one. The registry resolves the single-argument overload - an untyped column is
        // classified as ANY and matches its BOOLEAN parameter - and the producer is what rejects it, with a message
        QLParserException e = assertThrows(QLParserException.class, () -> parseExp("count(a)"));
        assertEquals("1:0 count() expects argument 1 to be BOOLEAN, got: a", e.getMessage());
    }

    @ParameterizedTest
    @MethodSource
    public void nestedCall(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> nestedCall() {
        return Stream.of(

                // a registry function inside another registry function
                arguments("sum(abs(int(a)))", $int("a").abs().sum()),
                arguments("scale(abs(int(a)), 2)", $int("a").abs().castAsDecimal().scale(2)),
                arguments("castAsInt(len(a)) + 1", $col("a").castAsStr().len().castAsInt().add($intVal(1))),

                // a still-grammar cast inside a registry function, and the other way around
                arguments("len(castAsStr(int(a)))", $int("a").castAsStr().len()),
                arguments("year(castAsDate('2020-01-01'))", $strVal("2020-01-01").castAsDate().year()),

                // a registry function on both sides of a relation
                arguments("abs(int(x)) > count()", $int("x").abs().gt(count())),
                arguments("cumSum(int(a)) > scale(int(b), 2)",
                        $int("a").cumSum().gt($int("b").castAsDecimal().scale(2)))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "castAsInt(a)",
            "castAsLong(a)",
            "castAsDecimal(a)",
            "year(castAsDate('2020-01-01'))",
            "millisecond(castAsTime('12:00:00.123'))",
            "concat(a, b)"
    })
    public void toQLRoundTrip(String text) {

        // "toQL()" of these calls is valid QL that the parser - now resolving the name through the registry -
        // rebuilds into the same expression.
        //
        // Not every moved function round-trips, and none of that is new in this change: "toQL()" is a display form,
        // not a QL serializer. It renders a typed column reference as a bare name ("int(a)" -> "a"), drops the
        // filter of an aggregate ("sum(a, b)" -> "sum(a)"), and emits text that is not a call at all for "count"
        // ("count") and for "scale" ("castAsDecimal(a) scale 2"). See the QL documentation follow-up.
        Exp<?> exp = parseExp(text);
        assertEquals(exp, parseExp(exp.toQL()));
    }
}
