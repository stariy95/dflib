package org.dflib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LongSeries_DescribeTest {

    @Test
    public void test() {
        SeriesInfo info = Series.ofLong(5L, 1L, 3L).describe();
        assertEquals(new SeriesInfo(Long.TYPE, Boolean.FALSE, null, 1., 3., 5.), info);
    }

    // "min" and "max" of an empty Series are zeros, and "avg" is NaN. Not particularly meaningful, but this is
    // the existing aggregation behavior
    @Test
    public void empty() {
        SeriesInfo info = Series.ofLong().describe();
        assertEquals(new SeriesInfo(Long.TYPE, Boolean.FALSE, null, 0., Double.NaN, 0.), info);
    }

    @Test
    public void singleValue() {
        SeriesInfo info = Series.ofLong(7L).describe();
        assertEquals(new SeriesInfo(Long.TYPE, Boolean.FALSE, null, 7., 7., 7.), info);
    }

    @Test
    public void negatives() {
        SeriesInfo info = Series.ofLong(-5L, 1L, -3L).describe();
        assertEquals(new SeriesInfo(Long.TYPE, Boolean.FALSE, null, -5., -7. / 3., 1.), info);
    }
}
