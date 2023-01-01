package com.nhl.dflib;

import com.nhl.dflib.unit.SeriesAsserts;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Series_ByElementTest {

    @Test
    public void test() {

        Series<String> s = Series
                .byElement(Extractor.<String>$col()).capacity(5).appendData()
                .append(List.of("a", "c", "e"))
                .toSeries();

        new SeriesAsserts(s).expectData("a", "c", "e");
    }

    @Test
    public void test_Int() {

        Series<Integer> s = Series
                .byElement(Extractor.$int((String str) -> Integer.parseInt(str))).capacity(5).appendData()
                .append(List.of("1", "55", "6"))
                .toSeries();

        assertTrue(s instanceof IntSeries);
        new SeriesAsserts(s).expectData(1, 55, 6);
    }
}