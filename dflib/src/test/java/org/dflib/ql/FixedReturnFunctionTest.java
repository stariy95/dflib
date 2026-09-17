package org.dflib.ql;

import org.dflib.Exp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.dflib.Exp.$boolVal;
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

    @Test
    public void fieldFn_null_throws() {
        QLParserException year = assertThrows(QLParserException.class, () -> parseExp("year(null)"));
        assertEquals("1:0 No overload of year matches year(const OBJECT)."
                + " Available: year(DATE), year(DATETIME), year(OFFSETDATETIME)."
                + " Argument 1 is untyped; cast it to one of [DATE, DATETIME, OFFSETDATETIME], e.g. castAsDate(..)",
                year.getMessage());

        QLParserException hour = assertThrows(QLParserException.class, () -> parseExp("hour(?)", new Object[]{null}));
        assertEquals("1:0 No overload of hour matches hour(const OBJECT)."
                + " Available: hour(TIME), hour(DATETIME), hour(OFFSETDATETIME)."
                + " Argument 1 is untyped; cast it to one of [TIME, DATETIME, OFFSETDATETIME], e.g. castAsTime(..)",
                hour.getMessage());
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
                        $int("a").cumSum().gt($int("b").castAsDecimal().scale(2))),
                arguments("count(castAsBool(a))", count($col("a").castAsBool())),
                arguments("castAsBool(a) = true", $col("a").castAsBool().eq($boolVal(true))),
                arguments("shift(abs(int(a)), 1)", $int("a").abs().shift(1)),
                arguments("abs(abs(int(a)))", $int("a").abs().abs()),
                arguments("if(castAsBool(a), abs(int(b)), 0)",
                        Exp.ifExp($col("a").castAsBool(), $int("b").abs(), Exp.$val(0)))
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
