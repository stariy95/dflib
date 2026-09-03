package org.dflib.ql;

import org.dflib.Environment;
import org.dflib.Exp;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.dflib.Exp.$date;
import static org.dflib.Exp.$dateTime;
import static org.dflib.Exp.$dateTimeVal;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$intVal;
import static org.dflib.Exp.$str;
import static org.dflib.Exp.$strVal;
import static org.dflib.ql.IdentityFunctions.identity;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NUMERIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The type of a call to a function that returns the type of one of its arguments is not known from its name, so such
 * a call can not be routed to one of the typed relation rules. The grammar handles it with a single untyped
 * "fnRelation" that dispatches on the expression the call produced. The built-in polymorphic names are covered by
 * {@link PolymorphicBuiltinTest}; this test exercises the rule in isolation, against a registry with one function
 * of a known shape, so that a failure points at the rule rather than at a built-in's signature.
 * <p>
 * That stand-in is registered by {@link IdentityFunctions}: one fixed-return overload per receiver type, the way
 * every built-in is written.
 */
public class FnRelationTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void setUpFunctions() {
        this.originalFunctions = Environment.commonEnv().getQLFunctions();

        // "f" returns its first argument unchanged, which makes the type of a call to it depend on the call site
        QLFunctions.Builder builder = QLFunctions.builder();
        identity(builder, "f");
        identity(builder, "f", new Arg(NUMERIC, true));
        Environment.setQLFunctions(builder.build());
    }

    @AfterEach
    public void restoreFunctions() {
        Environment.setQLFunctions(originalFunctions);
    }

    @Test
    public void comparison_Numeric() {
        assertEquals($int("a").gt($intVal(5)), Exp.parseExp("f(int(a), 1) > 5"));
        assertEquals($int("a").le($intVal(5)), Exp.parseExp("f(int(a), 1) <= 5"));
        assertEquals($int("a").ne($intVal(5)), Exp.parseExp("f(int(a), 1) != 5"));
    }

    @Test
    public void comparison_DateTimeAgainstParameter() {

        // the right-hand side is a parameter, so nothing but the left-hand side can decide the type of the
        // comparison. This is the case the untyped relation exists for
        LocalDateTime dt = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        assertEquals($dateTime("a").gt($dateTimeVal(dt)), Exp.parseExp("f(dateTime(a), 1) > ?", dt));
    }

    @Test
    public void comparison_String() {
        assertEquals($str("a").eq($strVal("x")), Exp.parseExp("f(str(a)) = 'x'"));
    }

    @Test
    public void comparison_TemporalStringLiteral() {

        // a typed temporal relation accepts an ISO-8601 string literal in place of a temporal expression, and so
        // must the untyped one
        assertEquals($date("a").gt("2024-01-01"), Exp.parseExp("f(date(a)) > '2024-01-01'"));
    }

    @Test
    public void between() {
        assertEquals($int("a").between($intVal(1), $intVal(5)), Exp.parseExp("f(int(a), 1) between 1 and 5"));
        assertEquals($int("a").notBetween($intVal(1), $intVal(5)),
                Exp.parseExp("f(int(a), 1) not between 1 and 5"));
        assertEquals($date("a").between("2024-01-01", "2024-12-31"),
                Exp.parseExp("f(date(a)) between '2024-01-01' and '2024-12-31'"));
    }

    @Test
    public void in() {
        assertEquals($str("a").in("x", "y"), Exp.parseExp("f(str(a)) in ('x', 'y')"));
        assertEquals($int("a").notIn(1, 2), Exp.parseExp("f(int(a), 1) not in (1, 2)"));
        assertEquals($date("a").in(java.time.LocalDate.parse("2024-01-01")),
                Exp.parseExp("f(date(a)) in ('2024-01-01')"));
    }
}
