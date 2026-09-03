package org.dflib.ql;

import org.dflib.Environment;
import org.dflib.Exp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.dflib.ql.IdentityFunctions.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ParseErrorReportingTest {

    @ParameterizedTest
    @MethodSource
    public void syntaxError_MessageUnchanged(String text, String expectedMessage) {
        QLParserException e = assertThrows(QLParserException.class, () -> Exp.parseExp(text));
        assertEquals(expectedMessage, e.getMessage());
    }

    static Stream<Arguments> syntaxError_MessageUnchanged() {
        return Stream.of(
                arguments("str(1) in ()", "line 1:11 mismatched input ')' expecting {'?', STRING_LITERAL}"),
                arguments("'hello' > 'world'", "line 1:8 mismatched input '>' expecting {<EOF>, 'as'}"),
                arguments("split(, ',')", "line 1:6 no viable alternative at input 'split(,'"),
                arguments("int(1) +", "line 1:8 no viable alternative at input 'int(1)+'"),
                arguments("int(1) = null",
                        "line 1:7 mismatched input '=' expecting {<EOF>, '+', '-', '*', '/', '%', 'as'}")
        );
    }

    @Test
    public void semanticError_UnknownFunction() {
        QLParserException e = assertThrows(QLParserException.class, () -> Exp.parseExp("foo(1, 2)"));
        assertEquals("Unknown function `foo` at 1:0", e.getMessage());
    }

    @Test
    public void semanticError_NoMatchingOverload() {
        QLParserException e = assertThrows(QLParserException.class, () -> Exp.parseExp("substr('example')"));
        assertEquals("1:0 Function substr([const STRING]) not found", e.getMessage());

        QLParserException nested = assertThrows(QLParserException.class, () -> Exp.parseExp("len(substr('a'))"));
        assertEquals("1:4 Function substr([const STRING]) not found", nested.getMessage());
    }

    @Test
    public void semanticError_ProducerRejectsArgument() {
        QLParserException e = assertThrows(QLParserException.class, () -> Exp.parseExp("year(str(a))"));
        assertEquals("1:0 Function year([STRING]) not found", e.getMessage());

        QLParserException scale = assertThrows(QLParserException.class, () -> Exp.parseExp("scale(int(a), 1.5)"));
        assertEquals("1:0 Not an integer constant: 1.5", scale.getMessage());
    }

    @Test
    public void semanticError_WrongReturnType() {
        QLFunctions original = Environment.commonEnv().getQLFunctions();
        try {
            Environment.setQLFunctions(identity(QLFunctions.builder(), "f").build());

            QLParserException num = assertThrows(QLParserException.class, () -> Exp.parseExp("f(str(a)) + 1"));
            assertEquals("f(...) at 1:0 returns a string expression; a numeric expression is required here",
                    num.getMessage());

            QLParserException date = assertThrows(QLParserException.class, () -> Exp.parseExp("date(a) > f(str(b))"));
            assertEquals("f(...) at 1:10 returns a string expression; a date expression is required here",
                    date.getMessage());
        } finally {
            Environment.setQLFunctions(original);
        }
    }

    @Test
    public void parameters_ReadInOrder() {
        assertEquals(Exp.$int("a").gt(Exp.$intVal(1)).and(Exp.$str("b").eq(Exp.$strVal("x"))),
                Exp.parseExp("int(a) > ? and str(b) = ?", 1, "x"));
    }
}
