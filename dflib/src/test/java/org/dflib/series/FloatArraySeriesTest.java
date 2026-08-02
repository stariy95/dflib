package org.dflib.series;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FloatArraySeriesTest {

    @Test
    public void getFloat() {
        FloatArraySeries s = new FloatArraySeries(1.f, 2.1f);
        assertEquals(1f, s.getFloat(0), 0.001f);
        assertEquals(2.1f, s.getFloat(1), 0.001f);
    }

    @Test
    public void min() {
        FloatArraySeries s = new FloatArraySeries(3.2f, 1.f, 2.1f);
        assertEquals(1f, s.min(), 0.001f);
    }

    @Test
    public void min_SingleValue() {
        FloatArraySeries s = new FloatArraySeries(3.2f);
        assertEquals(3.2f, s.min(), 0.001f);
    }

    @Test
    public void min_Empty() {
        FloatArraySeries s = new FloatArraySeries();
        assertEquals(0f, s.min(), 0.001f);
    }

    @Test
    public void max() {
        FloatArraySeries s = new FloatArraySeries(3.2f, 1.f, 2.1f);
        assertEquals(3.2f, s.max(), 0.001f);
    }
}
