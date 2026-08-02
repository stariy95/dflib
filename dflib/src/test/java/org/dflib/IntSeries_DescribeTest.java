package org.dflib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntSeries_DescribeTest {

    @Test
    public void test() {
        SeriesInfo info = Series.ofInt(5, 1, 3).describe();
        assertEquals(new SeriesInfo(Integer.TYPE, Boolean.FALSE, null, 1., 3., 5.), info);
    }

    // "min" and "max" of an empty Series are zeros, and "avg" is NaN. Not particularly meaningful, but this is
    // the existing aggregation behavior
    @Test
    public void empty() {
        SeriesInfo info = Series.ofInt().describe();
        assertEquals(new SeriesInfo(Integer.TYPE, Boolean.FALSE, null, 0., Double.NaN, 0.), info);
    }

    @Test
    public void singleValue() {
        SeriesInfo info = Series.ofInt(7).describe();
        assertEquals(new SeriesInfo(Integer.TYPE, Boolean.FALSE, null, 7., 7., 7.), info);
    }

    @Test
    public void negatives() {
        SeriesInfo info = Series.ofInt(-5, 1, -3).describe();
        assertEquals(new SeriesInfo(Integer.TYPE, Boolean.FALSE, null, -5., -7. / 3., 1.), info);
    }
}
