package org.dflib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BooleanSeries_DescribeTest {

    @Test
    public void test() {
        SeriesInfo info = Series.ofBool(true, false, true).describe();
        assertEquals(new SeriesInfo(Boolean.TYPE, Boolean.FALSE, null, null, null, null), info);
    }

    @Test
    public void allTrue() {
        SeriesInfo info = Series.ofBool(true, true).describe();
        assertEquals(new SeriesInfo(Boolean.TYPE, Boolean.FALSE, null, null, null, null), info);
    }

    @Test
    public void empty() {
        SeriesInfo info = Series.ofBool().describe();
        assertEquals(new SeriesInfo(Boolean.TYPE, Boolean.FALSE, null, null, null, null), info);
    }
}
