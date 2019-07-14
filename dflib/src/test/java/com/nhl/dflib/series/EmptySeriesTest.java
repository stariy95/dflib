package com.nhl.dflib.series;

import com.nhl.dflib.BooleanSeries;
import com.nhl.dflib.Series;
import com.nhl.dflib.unit.SeriesAsserts;
import org.junit.Test;

import java.util.Comparator;

public class EmptySeriesTest {

    @Test
    public void testFilter() {
        Series<Object> s = new EmptySeries<>()
                .filter(BooleanSeries.forBooleans(true, false));

        new SeriesAsserts(s).expectData();
    }

    @Test
    public void testSort() {
        Series<Object> s = new EmptySeries<>()
                .sort(Comparator.comparing(x -> x.toString()));

        new SeriesAsserts(s).expectData();
    }
}
