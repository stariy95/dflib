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
 * Built-in functions whose return type is fixed by their name.
 */
public class FixedReturnFunctionTest {

    @Test
    public void fieldFn_boundParameter() {
        LocalDateTime ldt = LocalDateTime.of(2020, 1, 2, 3, 4, 5);

        assertEquals($dateTimeVal(ldt).year(), parseExp("year(?)", ldt));
        assertEquals($dateTimeVal(ldt).hour(), parseExp("hour(?)", ldt));
    }

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
                arguments("sum(abs(int(a)))", $int("a").abs().sum()),
                arguments("scale(abs(int(a)), 2)", $int("a").abs().castAsDecimal().scale(2)),
                arguments("castAsInt(len(a)) + 1", $col("a").castAsStr().len().castAsInt().add($intVal(1))),
                arguments("len(castAsStr(int(a)))", $int("a").castAsStr().len()),
                arguments("year(castAsDate('2020-01-01'))", $strVal("2020-01-01").castAsDate().year()),
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
        Exp<?> exp = parseExp(text);
        assertEquals(exp, parseExp(exp.toQL()));
    }
}
