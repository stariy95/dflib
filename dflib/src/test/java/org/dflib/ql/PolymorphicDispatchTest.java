package org.dflib.ql;

import org.dflib.Environment;
import org.dflib.Exp;
import org.dflib.ql.QLFunctionDescriptor.Arg;
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
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.ANY;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NUMERIC;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.OBJECT;
import static org.dflib.ql.QLFunctionSignature.signature;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which grammar rule a call of a polymorphic function lands in, depending on the syntax around the call.
 */
public class PolymorphicDispatchTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void setUpFunctions() {
        this.originalFunctions = Environment.commonEnv().getQLFunctions();

        QLFunctions.Builder builder = QLFunctions.builder();
        identity(builder, "pmin");
        identity(builder, "plusLike", new Arg(NUMERIC, true));

        Environment.setQLFunctions(builder
                .function("foo", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsInt()))
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
        assertEquals($str("a").split(","), parseExp("split(str(a), ',')"));
        assertEquals($col("a").first(), parseExp("anyLike(a)"));
    }

    @Test
    public void polymorphic_Bare_Untyped() {
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
        assertEquals($int("a").add($intVal(1)), parseExp("(pmin(int(a))) + 1"));
        assertEquals($intVal(1).add($int("a").mul($intVal(2))), parseExp("1 + (pmin(int(a))) * 2"));
    }

    @Test
    public void polymorphic_Comparison_FnRelation() {
        assertEquals($int("a").gt($intVal(5)), parseExp("pmin(int(a)) > 5"));
        assertEquals($str("a").eq($strVal("x")), parseExp("pmin(str(a)) = 'x'"));
        assertEquals($date("a").between("2024-01-01", "2024-12-31"),
                parseExp("pmin(date(a)) between '2024-01-01' and '2024-12-31'"));
        assertEquals($col("x").eq($col("y")), parseExp("pmin(x) = pmin(y)"));
        assertEquals($int("a").notIn(1, 2), parseExp("pmin(int(a)) not in (1, 2)"));
    }

    @Test
    public void polymorphic_ParenthesizedComparison_TypedRelation() {
        assertEquals($int("a").gt($intVal(5)), parseExp("(pmin(int(a))) > 5"));
    }

    @Test
    public void polymorphic_OperatorThenComparison_TypedRelation() {
        assertEquals($int("a").add($intVal(1)).gt($intVal(5)), parseExp("pmin(int(a)) + 1 > 5"));
    }

    @Test
    public void polymorphic_RightHandSideOfTypedRelation() {
        assertEquals($intVal(5).gt($int("a")), parseExp("5 > pmin(int(a))"));
        assertEquals($int("b").le($int("a")), parseExp("int(b) <= pmin(int(a))"));
    }

    @Test
    public void polymorphic_TypedArgumentOfAGrammarRule() {
        assertEquals($date("a").year(), parseExp("year(pmin(date(a)))"));
    }

    @Test
    public void polymorphic_AsAnArgument_Untyped() {
        assertEquals($col("x").castAsInt().add($intVal(1)), parseExp("foo(pmin(x)) + 1"));
        assertEquals($int("a"), parseExp("pmin(plusLike(int(a), 1))"));
        assertEquals($date("a"), parseExp("pmin(plusLike(date(a), 1))"));
    }
}
