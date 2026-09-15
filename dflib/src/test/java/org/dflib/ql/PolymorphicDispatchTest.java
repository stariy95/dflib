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
import static org.dflib.ql.IdentityFunctions.identity;
import static org.dflib.ql.TypeClassifier.ANY;
import static org.dflib.ql.TypeClassifier.NUMERIC;
import static org.dflib.ql.TypeClassifier.OBJECT;
import static org.dflib.ql.DescriptorBuilder.descriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Calls of polymorphic functions in various syntactic positions: the function is resolved by its argument types, and
 * the surrounding operator is then checked against the type of the result.
 */
public class PolymorphicDispatchTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void setUpFunctions() {
        this.originalFunctions = Environment.commonEnv().getQLFunctions();

        QLFunctions.Builder builder = QLFunctions.builder();
        identity(builder, "pmin");
        identity(builder, "plusLike", new QLFunctionArg(NUMERIC, true));

        Environment.setQLFunctions(builder
                .function(descriptor("foo")
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsInt()))
                .function(descriptor("anyLike")
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
    public void fixedReturn() {
        assertEquals($int("a").abs(), parseExp("abs(int(a))"));
        assertEquals($int("a").abs().sum(), parseExp("sum(abs(int(a)))"));
        assertEquals($int("a").abs().castAsDecimal().scale(2), parseExp("scale(abs(int(a)), 2)"));
        assertEquals(count($col("x").castAsBool()), parseExp("count(castAsBool(x))"));
        assertEquals($int("a").abs().shift(1), parseExp("shift(abs(int(a)), 1)"));
        assertEquals(Exp.ifExp($col("a").castAsBool(), $intVal(1), $intVal(2)),
                parseExp("if(castAsBool(a), 1, 2)"));
    }

    @Test
    public void noTypedReturn() {
        assertEquals($str("a").split(","), parseExp("split(str(a), ',')"));
        assertEquals($col("a").first(), parseExp("anyLike(a)"));
    }

    @Test
    public void polymorphic_Bare() {
        assertEquals($str("a"), parseExp("pmin(str(a))"));
        assertEquals($int("a"), parseExp("pmin(int(a))"));
        assertEquals($col("a").first(), parseExp("pmin(anyLike(a))"));
    }

    @Test
    public void polymorphic_Operator() {
        assertEquals($int("a").add($intVal(1)), parseExp("pmin(int(a)) + 1"));
        assertEquals($int("a").mul($intVal(2)), parseExp("pmin(int(a)) * 2"));
        assertEquals($bool("a").and($bool("b")), parseExp("pmin(bool(a)) and bool(b)"));
    }

    @Test
    public void polymorphic_PrefixOperator() {
        assertEquals(Exp.not($bool("a")), parseExp("not pmin(bool(a))"));
        assertEquals(Exp.not($bool("a")), parseExp("not (pmin(bool(a)))"));
        assertEquals($int("a").negate(), parseExp("- pmin(int(a))"));
        assertEquals($intVal(1).sub($int("a")), parseExp("1 - pmin(int(a))"));
    }

    @Test
    public void polymorphic_Parenthesized() {
        assertEquals($int("a").add($intVal(1)), parseExp("(pmin(int(a))) + 1"));
        assertEquals($intVal(1).add($int("a").mul($intVal(2))), parseExp("1 + (pmin(int(a))) * 2"));
    }

    @Test
    public void polymorphic_Comparison() {
        assertEquals($int("a").gt($intVal(5)), parseExp("pmin(int(a)) > 5"));
        assertEquals($str("a").eq($strVal("x")), parseExp("pmin(str(a)) = 'x'"));
        assertEquals($date("a").between("2024-01-01", "2024-12-31"),
                parseExp("pmin(date(a)) between '2024-01-01' and '2024-12-31'"));
        assertEquals($col("x").eq($col("y")), parseExp("pmin(x) = pmin(y)"));
        assertEquals($int("a").notIn(1, 2), parseExp("pmin(int(a)) not in (1, 2)"));
    }

    @Test
    public void polymorphic_ParenthesizedComparison() {
        assertEquals($int("a").gt($intVal(5)), parseExp("(pmin(int(a))) > 5"));
    }

    @Test
    public void polymorphic_OperatorThenComparison() {
        assertEquals($int("a").add($intVal(1)).gt($intVal(5)), parseExp("pmin(int(a)) + 1 > 5"));
    }

    @Test
    public void polymorphic_RightHandSideOfComparison() {
        assertEquals($intVal(5).gt($int("a")), parseExp("5 > pmin(int(a))"));
        assertEquals($int("b").le($int("a")), parseExp("int(b) <= pmin(int(a))"));
    }

    @Test
    public void polymorphic_TypedArgumentOfACall() {
        assertEquals($date("a").year(), parseExp("year(pmin(date(a)))"));
    }

    @Test
    public void polymorphic_AsAnArgument() {
        assertEquals($col("x").castAsInt().add($intVal(1)), parseExp("foo(pmin(x)) + 1"));
        assertEquals($int("a"), parseExp("pmin(plusLike(int(a), 1))"));
        assertEquals($date("a"), parseExp("pmin(plusLike(date(a), 1))"));
    }
}
