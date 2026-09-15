package org.dflib.ql;

import org.dflib.Environment;
import org.dflib.Exp;
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
import static org.dflib.ql.TypeClassifier.NUMERIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comparisons whose left-hand side is a call of a polymorphic function: the comparison is built by dispatching on the
 * type of the expression the call produced.
 */
public class CallComparisonTest {

    private QLFunctions originalFunctions;

    @BeforeEach
    public void setUpFunctions() {
        this.originalFunctions = Environment.commonEnv().getQLFunctions();

        QLFunctions.Builder builder = QLFunctions.builder();
        identity(builder, "f");
        identity(builder, "f", new QLFunctionArg(NUMERIC, true));
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
        LocalDateTime dt = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        assertEquals($dateTime("a").gt($dateTimeVal(dt)), Exp.parseExp("f(dateTime(a), 1) > ?", dt));
    }

    @Test
    public void comparison_String() {
        assertEquals($str("a").eq($strVal("x")), Exp.parseExp("f(str(a)) = 'x'"));
    }

    @Test
    public void comparison_TemporalStringLiteral() {
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
