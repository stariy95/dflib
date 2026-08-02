package org.dflib;

import org.dflib.unit.DataFrameAsserts;
import org.junit.jupiter.api.Test;

public class ColumnSet_Select_ColsHeadTest {

    @Test
    public void withinBounds() {
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                        1, "x",
                        2, "y")
                .colsHead(1).select();

        new DataFrameAsserts(df, "a")
                .expectHeight(2)
                .expectRow(0, 1)
                .expectRow(1, 2);
    }

    @Test
    public void zero() {
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                        1, "x",
                        2, "y")
                .colsHead(0).select();

        new DataFrameAsserts(df).expectHeight(0);
    }

    @Test
    public void outOfBounds() {
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                        1, "x",
                        2, "y")
                .colsHead(4).select();

        new DataFrameAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, 1, "x")
                .expectRow(1, 2, "y");
    }

    @Test
    public void negative1() {
        DataFrame df = DataFrame.foldByRow("a", "c", "b").of(
                        1, "e", "x",
                        2, "k", "y")
                .colsHead(-1).select();

        new DataFrameAsserts(df, "c", "b")
                .expectHeight(2)
                .expectRow(0, "e", "x")
                .expectRow(1, "k", "y");
    }

    @Test
    public void negative2() {
        DataFrame df = DataFrame.foldByRow("a", "c", "b").of(
                        1, "e", "x",
                        2, "k", "y")
                .colsHead(-2).select();

        new DataFrameAsserts(df, "b")
                .expectHeight(2)
                .expectRow(0, "x")
                .expectRow(1, "y");
    }

    @Test
    public void negativeOutOfBounds() {
        DataFrame df = DataFrame.foldByRow("a", "c", "b").of(
                        1, "e", "x",
                        2, "k", "y")
                .colsHead(-4).select();

        new DataFrameAsserts(df).expectHeight(0);
    }
}
