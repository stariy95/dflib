package org.dflib.ql;

import org.dflib.Exp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.dflib.Exp.*;
import static org.dflib.Exp.$str;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ShiftTest {

    @ParameterizedTest
    @MethodSource
    public void shift(String text, Exp<?> expected) {
        Exp<?> exp = parseExp(text);
        assertEquals(expected, exp);
    }

    static Stream<Arguments> shift() {
        return Stream.of(
                arguments("shift(int(1), 2)", $int(1).shift(2)),
                arguments("shift(int(1), -2)", $int(1).shift(-2)),
                arguments("shift(int(1), 2, 0)", $int(1).shift(2, 0)),
                arguments("shift(1 + 2, 1)", $intVal(1).add(2).shift(1)),
                arguments("shift(str(1), 1)", $str(1).shift(1)),
                arguments("shift(str(1), 1, 'default')", $str(1).shift(1, "default")),
                arguments("shift(str(1), -1)", $str(1).shift(-1)),
                arguments("shift(date(a), 1)", $date("a").shift(1)),
                arguments("shift(plusDays(date(1), 1), 1)", $date(1).plusDays(1).shift(1)),
                arguments("shift(a, 1, 'x')", $col("a").shift(1, "x")),
                arguments("shift(a, 2)", $col("a").shift(2))
        );
    }

    @Test
    public void shift_incompatibleFiller() {
        assertEquals("1:0 shift() filler of type STRING is not compatible with a NUMERIC expression: `int(1)`",
                assertThrows(QLParserException.class, () -> parseExp("shift(int(1), 2, 'replace')")).getMessage());

        assertEquals("1:0 shift() filler of type NUMERIC is not compatible with a STRING expression: a",
                assertThrows(QLParserException.class, () -> parseExp("shift(str(a), 1, 1)")).getMessage());
    }

    @Test
    public void shift_numericFillerOfAnyWidth() {
        assertEquals("shift(a,1,1.5)", parseExp("shift(int(a), 1, 1.5)").toQL());
        assertEquals($long("a").shift(1, 99L), parseExp("shift(long(a), 1, 99L)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SHIFT(int(1), 2)",
            "shift(int(1))",
            "shift(int(1), )",
            "shift(, 2)",
    })
    public void shift_throws(String text) {
        assertThrows(QLParserException.class, () -> parseExp(text));
    }
}
