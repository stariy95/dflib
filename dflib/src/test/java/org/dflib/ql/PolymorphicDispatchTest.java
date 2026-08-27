package org.dflib.ql;

import org.dflib.Environment;
import org.dflib.Exp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.dflib.Exp.$bool;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$date;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$str;
import static org.dflib.Exp.$strVal;
import static org.dflib.Exp.count;
import static org.dflib.Exp.parseExp;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.ANY;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NUMERIC;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.OBJECT;
import static org.dflib.ql.QLFunctionSignature.signature;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A call by a registered name is matched both by the untyped "fnCall" alternative of "expression" and by the
 * "fnCall" hook of every typed rule the name may return. The two are token-identical, so unless the grammar makes
 * them mutually exclusive every call site is an ambiguous decision - and an ambiguous full-context decision is
 * never cached by ANTLR, so it is re-predicted on every parse.
 * <p>
 * A name with a fixed return type is assigned to one of them by the name alone. A name that returns the type of one
 * of its arguments is assigned by the syntax around the call: an operator applied to its result means it has to be
 * typed, anything else leaves it untyped. This test pins where each call site lands. The functions it registers are
 * minimal stand-ins for the built-in shapes ("shift", "plusDays", "min", "first"), so that a failure points at the
 * dispatch rules rather than at a built-in's signature; the built-ins themselves are covered by
 * {@link PolymorphicBuiltinTest}.
 */
public class PolymorphicDispatchTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void setUpFunctions() {
        this.originalFunctions = Environment.commonEnv().getQLFunctions();

        Environment.setQLFunctions(QLFunctions.builder()

                // returns its argument unchanged, so the type of a call to it is the type of the argument
                .function("pmin", signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .as(args -> args.get(0)))

                // a polymorphic function of two arguments, in the shape of "plusDays(e, n)"
                .function("plusLike", signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0)))

                // a fixed numeric return, to check that a polymorphic call nested in it stays untyped
                .function("foo", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsInt()))

                // a return type that is only known at eval time - "first" and friends. No typed rule claims it
                .function("anyLike", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .as(args -> args.get(0).first()))
                .build());
    }

    @AfterEach
    public void restoreFunctions() {
        Environment.setQLFunctions(originalFunctions);
    }

    @Test
    public void fixedReturn_TypedRuleOnly() {

        // a name with a fixed typed return is not matched by the untyped alternative at all, so these decisions
        // have a single viable alternative
        assertEquals($int("a").abs(), parseExp("abs(int(a))"));
        assertEquals($int("a").abs().sum(), parseExp("sum(abs(int(a)))"));
        assertEquals($int("a").abs().castAsDecimal().scale(2), parseExp("scale(abs(int(a)), 2)"));
        assertEquals(count($col("x").castAsBool()), parseExp("count(castAsBool(x))"));
        assertEquals($int("a").abs().shift(1), parseExp("shift(abs(int(a)), 1)"));
        assertEquals(Exp.ifExp($col("a").castAsBool(), $intVal(1), $intVal(2)),
                parseExp("if(castAsBool(a), 1, 2)"));
    }

    @Test
    public void noTypedReturn_UntypedOnly() {

        // no typed rule claims a name that returns a plain object or a type known only at eval time
        assertEquals($str("a").split(","), parseExp("split(str(a), ',')"));
        assertEquals($col("a").first(), parseExp("anyLike(a)"));
    }

    @Test
    public void polymorphic_Bare_Untyped() {

        // nothing around the call demands a type, so it is resolved untyped and keeps whatever type its arguments
        // gave it
        assertEquals($str("a"), parseExp("pmin(str(a))"));
        assertEquals($int("a"), parseExp("pmin(int(a))"));
        assertEquals($col("a").first(), parseExp("pmin(anyLike(a))"));
    }

    @Test
    public void polymorphic_Operator_Typed() {

        assertEquals($int("a").add($intVal(1)), parseExp("pmin(int(a)) + 1"));
        assertEquals($int("a").mul($intVal(2)), parseExp("pmin(int(a)) * 2"));
        assertEquals($bool("a").and($bool("b")), parseExp("pmin(bool(a)) and bool(b)"));
    }

    @Test
    public void polymorphic_PrefixOperator_Typed() {

        assertEquals(Exp.not($bool("a")), parseExp("not pmin(bool(a))"));
        assertEquals(Exp.not($bool("a")), parseExp("not (pmin(bool(a)))"));
        assertEquals($int("a").negate(), parseExp("- pmin(int(a))"));
        assertEquals($intVal(1).sub($int("a")), parseExp("1 - pmin(int(a))"));
    }

    @Test
    public void polymorphic_Parenthesized_Typed() {

        // the parentheses around the call are skipped when looking for the operator applied to its result
        assertEquals($int("a").add($intVal(1)), parseExp("(pmin(int(a))) + 1"));
        assertEquals($intVal(1).add($int("a").mul($intVal(2))), parseExp("1 + (pmin(int(a))) * 2"));
    }

    @Test
    public void polymorphic_Comparison_FnRelation() {

        // a comparison of a polymorphic call is built by "fnRelation", which parses the call untyped and dispatches
        // on the expression it produced
        assertEquals($int("a").gt($intVal(5)), parseExp("pmin(int(a)) > 5"));
        assertEquals($str("a").eq($strVal("x")), parseExp("pmin(str(a)) = 'x'"));
        assertEquals($date("a").between("2024-01-01", "2024-12-31"),
                parseExp("pmin(date(a)) between '2024-01-01' and '2024-12-31'"));
        assertEquals($col("x").eq($col("y")), parseExp("pmin(x) = pmin(y)"));
        assertEquals($int("a").notIn(1, 2), parseExp("pmin(int(a)) not in (1, 2)"));
    }

    @Test
    public void polymorphic_ParenthesizedComparison_TypedRelation() {

        // "fnRelation" matches a call at its left edge, so a comparison of a parenthesized call is a typed relation
        assertEquals($int("a").gt($intVal(5)), parseExp("(pmin(int(a))) > 5"));
    }

    @Test
    public void polymorphic_OperatorThenComparison_TypedRelation() {

        // the operator makes the left-hand side a numeric expression, so this is a numeric relation, not "fnRelation"
        assertEquals($int("a").add($intVal(1)).gt($intVal(5)), parseExp("pmin(int(a)) + 1 > 5"));
    }

    @Test
    public void polymorphic_RightHandSideOfTypedRelation() {

        // the left-hand side decided the type of the comparison, so the right-hand side is parsed by a typed rule,
        // and the hook has to stay viable there
        assertEquals($intVal(5).gt($int("a")), parseExp("5 > pmin(int(a))"));
        assertEquals($int("b").le($int("a")), parseExp("int(b) <= pmin(int(a))"));
    }

    @Test
    public void polymorphic_TypedArgumentOfAGrammarRule() {

        // "year" declares a date argument, so the call in it must be viable as a date expression
        assertEquals($date("a").year(), parseExp("year(pmin(date(a)))"));
    }

    @Test
    public void polymorphic_AsAnArgument_Untyped() {

        // an argument position demands nothing, so the nested call stays untyped and is resolved by its own
        // arguments
        assertEquals($col("x").castAsInt().add($intVal(1)), parseExp("foo(pmin(x)) + 1"));
        assertEquals($int("a"), parseExp("pmin(plusLike(int(a), 1))"));
        assertEquals($date("a"), parseExp("pmin(plusLike(date(a), 1))"));
    }
}
