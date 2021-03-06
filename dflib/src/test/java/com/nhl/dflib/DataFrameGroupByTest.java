package com.nhl.dflib;

import com.nhl.dflib.aggregate.Aggregator;
import com.nhl.dflib.map.KeyMapper;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class DataFrameGroupByTest {

    @Test
    public void testGroupBy() {
        Index i = Index.withNames("a", "b");
        DataFrame df = DataFrame.fromSequence(i,
                1, "x",
                2, "y",
                1, "z",
                0, "a",
                1, "x");

        GroupBy gb = df.groupBy(KeyMapper.keyColumn("a"));
        assertNotNull(gb);

        assertEquals(3, gb.size());
        assertEquals(new HashSet<>(asList(0, 1, 2)), new HashSet<>(gb.groups()));

        new DFAsserts(gb.group(0), "a", "b")
                .expectHeight(1)
                .expectRow(0, 0, "a");

        new DFAsserts(gb.group(1), "a", "b")
                .expectHeight(3)
                .expectRow(0, 1, "x")
                .expectRow(1, 1, "z")
                .expectRow(2, 1, "x");

        new DFAsserts(gb.group(2), "a", "b")
                .expectHeight(1)
                .expectRow(0, 2, "y");
    }

    @Test
    public void testGroupBy_Empty() {
        Index i = Index.withNames("a", "b");
        DataFrame df = DataFrame.fromSequence(i);

        GroupBy gb = df.groupBy(KeyMapper.keyColumn("a"));
        assertNotNull(gb);

        assertEquals(0, gb.size());
        assertEquals(Collections.emptySet(), new HashSet<>(gb.groups()));
    }

    @Test
    public void testGroupBy_Agg() {
        Index i = Index.withNames("a", "b");
        DataFrame df1 = DataFrame.fromSequence(i,
                1, "x",
                2, "y",
                1, "z",
                0, "a",
                1, "x");

        DataFrame df = df1.groupBy("a").agg(Aggregator.sum("a"), Aggregator.concat("b", ";"));

        new DFAsserts(df, "a", "b")
                .expectHeight(3)
                .expectRow(0, 3L, "x;z;x")
                .expectRow(1, 2L, "y")
                .expectRow(2, 0L, "a");
    }

    @Test
    public void testGroupBy_Agg_MultipleAggregationsForKey() {
        Index i = Index.withNames("a", "b");
        DataFrame df1 = DataFrame.fromSequence(i,
                1, "x",
                2, "y",
                1, "y",
                0, "a",
                1, "x");

        DataFrame df = df1.groupBy("b")
                .agg(Aggregator.first("b"), Aggregator.sum("a"), Aggregator.median("a"));

        new DFAsserts(df, "b", "a", "a_")
                .expectHeight(3)
                .expectRow(0, "x", 2L, 1.)
                .expectRow(1, "y", 3L, 1.5)
                .expectRow(2, "a", 0L, 0.);
    }
}
