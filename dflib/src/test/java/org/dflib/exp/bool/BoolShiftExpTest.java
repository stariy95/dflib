package org.dflib.exp.bool;

import org.dflib.BooleanSeries;
import org.dflib.Condition;
import org.dflib.Series;
import org.dflib.exp.BaseExpTest;
import org.dflib.unit.BoolSeriesAsserts;
import org.junit.jupiter.api.Test;

import static org.dflib.Exp.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class BoolShiftExpTest extends BaseExpTest {

    @Test
    public void shift() {
        BooleanSeries s = Series.ofBool(true, false, true);

        new BoolSeriesAsserts($bool(0).shift(1).eval(s)).expectData(false, true, false);
        new BoolSeriesAsserts($bool(0).shift(2).eval(s)).expectData(false, false, true);
        new BoolSeriesAsserts($bool(0).shift(-1).eval(s)).expectData(false, true, false);
        new BoolSeriesAsserts($bool(0).shift(100).eval(s)).expectData(false, false, false);
        new BoolSeriesAsserts($bool(0).shift(-100).eval(s)).expectData(false, false, false);
        new BoolSeriesAsserts($bool(0).shift(Integer.MIN_VALUE).eval(s)).expectData(false, false, false);
    }

    @Test
    public void shift_Filler() {
        BooleanSeries s = Series.ofBool(false, false, false);

        new BoolSeriesAsserts($bool(0).shift(1, true).eval(s)).expectData(true, false, false);
        new BoolSeriesAsserts($bool(0).shift(-2, true).eval(s)).expectData(false, true, true);
        new BoolSeriesAsserts($bool(0).shift(1, null).eval(s)).expectData(false, false, false);
    }

    @Test
    public void shift_Condition() {
        Condition c = $int("a").gt(0).shift(1).not();
        BooleanSeries result = c.eval(Series.of(1, -1, 1));
        new BoolSeriesAsserts(result).expectData(true, false, true);
    }

    @Test
    public void toQL() {
        assertEquals("shift(a,1,false)", $bool("a").shift(1).toQL());
        assertEquals("shift(a,-1,true)", $bool("a").shift(-1, true).toQL());
    }

    @Test
    public void parse() {
        assertEquals($bool("a").shift(1, true), parseExp("shift(bool(a),1,true)"));
        assertEquals($bool("a").shift(1), parseExp("shift(bool(a),1)"));
        assertInstanceOf(Condition.class, parseExp("shift(bool(a),1)"));
    }

    @Test
    public void testEquals() {
        assertExpEquals(
                $bool("a").shift(1),
                $bool("a").shift(1),
                $bool("b").shift(1));

        assertExpEquals(
                $bool("a").shift(1),
                $bool("a").shift(1, false),
                $bool("a").shift(1, true));
    }

    @Test
    public void testHashCode() {
        assertExpHashCode(
                $bool("a").shift(1),
                $bool("a").shift(1, false),
                $bool("a").shift(2));
    }
}
