package org.dflib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FloatSeries_DescribeTest {

    @Test
    public void test() {
        SeriesInfo info = Series.ofFloat(5.5f, 1.5f, 3.5f).describe();
        assertEquals(new SeriesInfo(Float.TYPE, Boolean.FALSE, null, 1.5, 3.5, 5.5), info);
    }

    // "min" and "max" of an empty Series are zeros, and "avg" is NaN. Not particularly meaningful, but this is
    // the existing aggregation behavior
    @Test
    public void empty() {
        SeriesInfo info = Series.ofFloat().describe();
        assertEquals(new SeriesInfo(Float.TYPE, Boolean.FALSE, null, 0., Double.NaN, 0.), info);
    }

    @Test
    public void singleValue() {
        SeriesInfo info = Series.ofFloat(7.5f).describe();
        assertEquals(new SeriesInfo(Float.TYPE, Boolean.FALSE, null, 7.5, 7.5, 7.5), info);
    }

    @Test
    public void negatives() {
        SeriesInfo info = Series.ofFloat(-5.5f, 1.5f, -3.5f).describe();
        assertEquals(new SeriesInfo(Float.TYPE, Boolean.FALSE, null, -5.5, (double) (-7.5f / 3f), 1.5), info);
    }
}
