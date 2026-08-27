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
 * Pins the behaviour of the built-in functions whose name is shared between several receiver types, whose return
 * type is the type of an argument, or which are special forms - the last group to move out of the grammar into the
 * function registry.
 * <p>
 * While these were grammar rules, each of them lived under exactly one return type, and a call was routed to a rule
 * by its name before its arguments were known. That is what made a nested call of a different shape - the argument
 * of an aggregate, the receiver of a "plusX" or a "shift" - either unparseable or mis-routed. Resolving a call from
 * its actual arguments removes the restriction, and this test records what that changed.
 */
public class PolymorphicBuiltinTest {

    /**
     * A call nested inside a call of a shared name. The inner call is what decides the receiver type, and it was
     * only known after the outer rule had been committed to.
     */
    @ParameterizedTest
    @MethodSource
    public void nestedCallReceiver(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> nestedCallReceiver() {
        return Stream.of(

                // the receiver is a cast, whose token used to route the call to a rule of the cast's own type
                arguments("hour(castAsDateTime(x))", $col("x").castAsDateTime().hour()),
                arguments("plusDays(castAsDateTime(x), 1)", $col("x").castAsDateTime().plusDays(1)),
                arguments("min(castAsDate(x))", $col("x").castAsDate().min()),
                arguments("avg(castAsTime(x))", $col("x").castAsTime().avg()),
                arguments("median(castAsDateTime(x))", $col("x").castAsDateTime().median()),

                // the receiver is a string function: "min ( IDENTIFIER (" used to be token-viable for the numeric
                // aggregate, which was the lower-numbered alternative
                arguments("min(trim(a))", $col("a").trim().min()),
                arguments("max(lower(a))", $col("a").lower().max()),

                // the receiver is another polymorphic call
                arguments("plusDays(plusMonths(date(a), 1), 2)", $date("a").plusMonths(1).plusDays(2)),
                arguments("min(plusDays(date(a), 1))", $date("a").plusDays(1).min())
        );
    }

    /**
     * A polymorphic call in a position that demands a type. The syntax around the call is what tells the parser
     * whether the call is an operand of an operator, the left-hand side of a comparison, or neither.
     */
    @ParameterizedTest
    @MethodSource
    public void typedContinuation(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> typedContinuation() {
        return Stream.of(
                arguments("min(int(a)) + 1", $int("a").min().add($intVal(1))),
                arguments("avg(int(a)) * 2 > 5", $int("a").avg().mul($intVal(2)).gt($intVal(5))),
                arguments("- min(int(a))", $int("a").min().negate()),

                // a grouping parenthesis around the call does not hide the operator applied to it
                arguments("(min(int(a))) + 1", $int("a").min().add($intVal(1))),

                // a comparison of a polymorphic call is built by "fnRelation", dispatching on the expression the
                // call produced rather than on the call's name
                arguments("min(str(a)) = 'x'", $str("a").min().eq($strVal("x"))),
                arguments("avg(int(1)) <= 20", $int(1).avg().le($intVal(20))),
                arguments("min(int(a)) between 1 and 5", $int("a").min().between($intVal(1), $intVal(5))),
                arguments("min(int(a)) in (1, 2)", $int("a").min().in(1, 2))
        );
    }

    /**
     * The right-hand side of a comparison of a polymorphic call is parsed untyped, so a bare parameter on that side
     * is typed by the value bound to it rather than by the grammar.
     */
    @Test
    public void fnRelation_parameterRhs() {
        LocalDateTime v = LocalDateTime.of(2024, 1, 2, 3, 4);
        assertEquals(
                $dateTime("a").plusDays(1).gt($dateTimeVal(v)),
                parseExp("plusDays(dateTime(a), 1) > ?", v));
    }

    /**
     * Special forms. All three used to be grammar rules with hand-written per-type alternatives.
     */
    @ParameterizedTest
    @MethodSource
    public void specialForm(String text, Exp<?> expected) {
        assertEquals(expected, parseExp(text));
    }

    static Stream<Arguments> specialForm() {
        return Stream.of(

                // a temporal receiver of "shift" used to be rejected: the grammar had bool/num/str alternatives and
                // a generic one that accepted only a column reference, an aggregate or another special form
                arguments("shift(date(a), 1)", $date("a").shift(1)),
                arguments("shift(time(a), -1)", $time("a").shift(-1)),
                arguments("shift(plusDays(date(a), 1), 1)", $date("a").plusDays(1).shift(1)),

                arguments("first(a, bool(b))", $col("a").first($bool("b"))),
                arguments("last(a)", $col("a").last()),

                arguments("castAsDate(a, 'yyyy')", $col("a").castAsDate("yyyy")),

                // all four "vConcat" arities always build the 4-argument form, as the grammar rule did
                arguments("vConcat(a, ',')", $col("a").vConcat(null, ",", "", "")),
                arguments("vConcat(a, bool(b), ',')", $col("a").vConcat($bool("b"), ",", "", "")),
                arguments("vConcat(a, ',', '[', ']')", $col("a").vConcat(null, ",", "[", "]")),
                arguments("vConcat(a, bool(b), ',', '[', ']')", $col("a").vConcat($bool("b"), ",", "[", "]"))
        );
    }

    /**
     * A call that the registry resolves but whose producer rejects the arguments is a parse error carrying the
     * position of the call. These used to be syntax errors reported at the opening parenthesis, or - for the
     * receiver types the grammar had no alternative for - at the argument.
     */
    @ParameterizedTest
    @MethodSource
    public void producerError(String text, String message) {
        QLParserException e = assertThrows(QLParserException.class, () -> parseExp(text));
        assertEquals(message, e.getMessage());
    }

    static Stream<Arguments> producerError() {
        return Stream.of(

                // an untyped column has no type until eval, so no receiver-dispatching function accepts it
                arguments("year(a)", "1:0 year() is not supported for expression: a"),
                arguments("min(a)", "1:0 min() is not supported for expression: a"),
                arguments("plusDays(a, 1)", "1:0 plusDays() is not supported for expression: a"),

                // "avg" and "median" are not declared on StrExp
                arguments("avg(str(a))", "1:0 avg() is not supported for expression: a"),

                // a "plusX" count is a constant int, not any number
                arguments("plusDays(date(a), 1.5)", "1:0 Not an integer constant: 1.5"),

                // "if" needs a condition, and an untyped column is not one
                arguments("if(a, 1, 2)", "1:0 if() expects a boolean expression, got: a")
        );
    }

    /**
     * Inputs that stay parse errors.
     */
    @ParameterizedTest
    @ValueSource(strings = {

            // "shift" of a Condition produces a plain Exp<Boolean>, not a Condition, so it can not be an operand of
            // a boolean operator
            "not shift(bool(a), 1)",

            // OffsetDateTimeExp declares no aggregates
            "min(offsetDateTime(a))",

            // an aggregate of an untyped column on either side of a comparison
            "min(x) = min(y)",
    })
    public void throws_(String text) {
        assertThrows(QLParserException.class, () -> parseExp(text));
    }

    /**
     * "toQL" of an expression built by one of the moved functions is a call by the same name, and re-parsing it
     * yields the same expression.
     * <p>
     * Only the names whose "toQL" is a call at all are covered, and only over receivers that themselves survive a
     * round trip. Two pre-existing gaps are deliberately not fought here: a typed column reference serializes to a
     * bare name ("int(a)" becomes "a"), so anything applied to one comes back untyped; and "plusX" serializes as
     * "a plusDays 1", which is not QL syntax at all. Aggregates with a filter, "count" and "scale" have the same
     * problem and were already known to before this change.
     */
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
