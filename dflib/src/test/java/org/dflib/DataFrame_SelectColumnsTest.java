package org.dflib;

import org.dflib.unit.DataFrameAsserts;
import org.junit.jupiter.api.Test;

import static org.dflib.Exp.$col;
import static org.dflib.Exp.$int;

@Deprecated
public class DataFrame_SelectColumnsTest {

    @Test
    public void byCondition() {
        DataFrame df = DataFrame.foldByRow("x1", "b", "x2").of(
                1, "x", "z",
                2, "y", "a")
                .selectColumns(c -> c.startsWith("x"));

        new DataFrameAsserts(df, "x1", "x2")
                .expectHeight(2)
                .expectRow(0, 1, "z")
                .expectRow(1, 2, "a");
    }

    @Test
    public void byLabel() {
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y")
                .selectColumns("b");

        new DataFrameAsserts(df, "b")
                .expectHeight(2)
                .expectRow(0, "x")
                .expectRow(1, "y");
    }

    @Test
    public void withExp() {
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y")
                .selectColumns($col("b"), $int("a").mul($int("a")));

        new DataFrameAsserts(df, "b", "a * a")
                .expectHeight(2)
                .expectRow(0, "x", 1)
                .expectRow(1, "y", 4);
    }

    @Test
    public void duplicateColumn() {
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y")
                .selectColumns("b", "b", "b");

        new DataFrameAsserts(df, "b", "b_", "b__")
                .expectHeight(2)
                .expectRow(0, "x", "x", "x")
                .expectRow(1, "y", "y", "y");
    }

    @Test
    public void duplicateColumn_ByPos() {
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                        1, "x",
                        2, "y")
                .selectColumns(1, 1, 1);

        new DataFrameAsserts(df, "b", "b_", "b__")
                .expectHeight(2)
                .expectRow(0, "x", "x", "x")
                .expectRow(1, "y", "y", "y");
    }


    @Test
    public void byIndex() {

        DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", "m",
                2, "y", "n")
                .selectColumns(Index.of("b", "a"));

        new DataFrameAsserts(df, "b", "a")
                .expectHeight(2)
                .expectRow(0, "x", 1)
                .expectRow(1, "y", 2);
    }

    @Test
    public void withAsExp(){
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y")
                .selectColumns($col("b"), $int("a").mul($int("a")).as("c"));

        new DataFrameAsserts(df, "b", "c")
                .expectHeight(2)
                .expectRow(0, "x", 1)
                .expectRow(1, "y", 4);
    }
}