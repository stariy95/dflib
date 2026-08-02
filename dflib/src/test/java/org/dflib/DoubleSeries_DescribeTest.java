package org.dflib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoubleSeries_DescribeTest {

    @Test
    public void test() {
        SeriesInfo info = Series.ofDouble(5.5, 1.5, 3.5).describe();
        assertEquals(new SeriesInfo(Double.TYPE, Boolean.FALSE, null, 1.5, 3.5, 5.5), info);
    }

    // "min" and "max" of an empty Series are zeros, and "avg" is NaN. Not particularly meaningful, but this is
    // the existing aggregation behavior
    @Test
    public void empty() {
        SeriesInfo info = Series.ofDouble().describe();
        assertEquals(new SeriesInfo(Double.TYPE, Boolean.FALSE, null, 0., Double.NaN, 0.), info);
    }

    @Test
    public void singleValue() {
        SeriesInfo info = Series.ofDouble(7.5).describe();
        assertEquals(new SeriesInfo(Double.TYPE, Boolean.FALSE, null, 7.5, 7.5, 7.5), info);
    }

    @Test
    public void negatives() {
        SeriesInfo info = Series.ofDouble(-5.5, 1.5, -3.5).describe();
        assertEquals(new SeriesInfo(Double.TYPE, Boolean.FALSE, null, -5.5, -7.5 / 3., 1.5), info);
    }
}
