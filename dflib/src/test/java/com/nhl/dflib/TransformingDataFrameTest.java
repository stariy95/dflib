package com.nhl.dflib;

import com.nhl.dflib.map.DataRowMapper;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransformingDataFrameTest {

    private static final DataRowMapper SELF_MAPPER = (c, r) -> r;

    @Test
    public void testIterator() {

        Index i = Index.withNames("a", "b");

        TransformingDataFrame df = new TransformingDataFrame(
                i,
                DataFrame.fromRows(i, DataRow.row("one", 1), DataRow.row("two", 2)),
                SELF_MAPPER);

        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, "one", 1)
                .expectRow(1, "two", 2);
    }

    @Test
    public void testHead() {

        Index columns = Index.withNames("a", "b");

        DataFrame df = new TransformingDataFrame(
                columns,
                DataFrame.fromRows(columns, DataRow.row("one", 1), DataRow.row("two", 2), DataRow.row("three", 3)),
                SELF_MAPPER).head(2);

        new DFAsserts(df, columns)
                .expectHeight(2)
                .expectRow(0, "one", 1)
                .expectRow(1, "two", 2);
    }

    @Test
    public void testRenameColumn() {
        Index i = Index.withNames("a", "b");

        DataFrame df = new TransformingDataFrame(
                i,
                DataFrame.fromRows(i, DataRow.row("one", 1), DataRow.row("two", 2)),
                SELF_MAPPER).renameColumn("b", "c");

        new DFAsserts(df, "a", "c")
                .expectHeight(2)
                .expectRow(0, "one", 1)
                .expectRow(1, "two", 2);
    }

    @Test
    public void testMapColumn() {

        Index i = Index.withNames("a", "b");

        DataFrame df = new TransformingDataFrame(
                i,
                DataFrame.fromRows(i, DataRow.row("one", 1), DataRow.row("two", 2)),
                SELF_MAPPER).mapColumn("b", (c, r) -> c.get(r, 1).toString());

        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, "one", "1")
                .expectRow(1, "two", "2");
    }

    @Test
    public void testMap() {

        Index i = Index.withNames("a", "b");

        DataFrame df = new TransformingDataFrame(
                i,
                DataFrame.fromRows(i, DataRow.row("one", 1), DataRow.row("two", 2)),
                SELF_MAPPER)
                .map(i, (c, r) -> c.mapColumn(r, "a", (cx, rx) -> cx.get(rx, 0) + "_"));

        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, "one_", 1)
                .expectRow(1, "two_", 2);
    }

    @Test
    public void testMap_ChangeRowStructure() {

        Index i = Index.withNames("a", "b");
        Index i1 = Index.withNames("c", "d", "e");

        DataFrame df = new TransformingDataFrame(
                i,
                DataFrame.fromRows(i, DataRow.row("one", 1), DataRow.row("two", 2)),
                SELF_MAPPER)
                .map(i1, (c, r) -> DataRow.row(
                        r[0],
                        ((int) r[1]) * 10,
                        r[1]));

        new DFAsserts(df, i1)
                .expectHeight(2)
                .expectRow(0, "one", 10, 1)
                .expectRow(1, "two", 20, 2);
    }

    @Test
    public void testMap_ChangeRowStructure_Chained() {

        Index i = Index.withNames("a", "b");
        Index i1 = Index.withNames("c", "d", "e");
        Index i2 = Index.withNames("f", "g");

        DataFrame df = new TransformingDataFrame(
                i,
                DataFrame.fromRows(i, DataRow.row("one", 1), DataRow.row("two", 2)),
                SELF_MAPPER)
                .map(i1, (c, r) -> c.target(
                        r[0],
                        ((int) r[1]) * 10,
                        r[1]))
                .map(i2, (c, r) -> c.target(
                        r[0],
                        r[1]));

        new DFAsserts(df, i2)
                .expectHeight(2)
                .expectRow(0, "one", 10)
                .expectRow(1, "two", 20);
    }

    @Test
    public void testMap_ChangeRowStructure_EmptyDF() {

        Index i = Index.withNames("a", "b");
        Index i1 = Index.withNames("c", "d", "e");

        DataFrame df = new TransformingDataFrame(i, DataFrame.fromRows(i), SELF_MAPPER)
                .map(i1, (c, r) -> c.target(r[0], ((int) r[1]) * 10, r[1]));

        assertSame(i1, df.getColumns());

        new DFAsserts(df, i1).expectHeight(0);
    }

    @Test
    public void testToString() {
        Index i = Index.withNames("a", "b");
        DataFrame df = new TransformingDataFrame(i, DataFrame.fromRows(i,
                DataRow.row("one", 1),
                DataRow.row("two", 2),
                DataRow.row("three", 3),
                DataRow.row("four", 4)), SELF_MAPPER);

        assertEquals("TransformingDataFrame [{a:one,b:1},{a:two,b:2},{a:three,b:3},...]", df.toString());
    }

    @Test
    public void testZip() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new TransformingDataFrame(i1, DataFrame.fromRows(i1,
                DataRow.row(1),
                DataRow.row(2)), SELF_MAPPER);

        Index i2 = Index.withNames("b");
        DataFrame df2 = new TransformingDataFrame(i2, DataFrame.fromRows(i2,
                DataRow.row(10),
                DataRow.row(20)), SELF_MAPPER);

        DataFrame df = df1.zip(df2);
        new DFAsserts(df, "a", "b")
                .expectHeight(2)
                .expectRow(0, 1, 10)
                .expectRow(1, 2, 20);
    }

    @Test
    public void testZip_Self() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new TransformingDataFrame(i1, DataFrame.fromRows(i1,
                DataRow.row(1),
                DataRow.row(2)), SELF_MAPPER);

        DataFrame df = df1.zip(df1);

        new DFAsserts(df, "a", "a_")
                .expectHeight(2)
                .expectRow(0, 1, 1)
                .expectRow(1, 2, 2);
    }

    @Test
    public void testZip_LeftIsShorter() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new TransformingDataFrame(i1, DataFrame.fromRows(i1, DataRow.row(2)), SELF_MAPPER);

        Index i2 = Index.withNames("b");
        DataFrame df2 = new TransformingDataFrame(i2, DataFrame.fromRows(i2,
                DataRow.row(10),
                DataRow.row(20)), SELF_MAPPER);

        DataFrame df = df1.zip(df2);
        new DFAsserts(df, "a", "b")
                .expectHeight(1)
                .expectRow(0, 2, 10);
    }

    @Test
    public void testZip_RightIsShorter() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new TransformingDataFrame(i1, DataFrame.fromRows(i1,
                DataRow.row(2)), SELF_MAPPER);

        Index i2 = Index.withNames("b");
        DataFrame df2 = new TransformingDataFrame(i2, DataFrame.fromRows(i2,
                DataRow.row(10),
                DataRow.row(20)), SELF_MAPPER);

        DataFrame df = df2.zip(df1);
        new DFAsserts(df, "b", "a")
                .expectHeight(1)
                .expectRow(0, 10, 2);
    }

    @Test
    public void testFilter() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new TransformingDataFrame(i1, DataFrame.fromRows(i1,
                DataRow.row(10),
                DataRow.row(20)), SELF_MAPPER);

        DataFrame df = df1.filter((c, r) -> ((int) c.get(r, 0)) > 15);
        new DFAsserts(df, "a")
                .expectHeight(1)
                .expectRow(0, 20);
    }

    @Test
    public void testFilterColumn_Name() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new TransformingDataFrame(i1, DataFrame.fromRows(i1,
                DataRow.row(10),
                DataRow.row(20)), SELF_MAPPER);

        DataFrame df = df1.filterColumn("a", (Integer v) -> v > 15);
        new DFAsserts(df, "a")
                .expectHeight(1)
                .expectRow(0, 20);
    }

    @Test
    public void testFilterColumn_Pos() {

        Index i1 = Index.withNames("a");
        DataFrame df1 = new TransformingDataFrame(i1, DataFrame.fromRows(i1,
                DataRow.row(10),
                DataRow.row(20)), SELF_MAPPER);

        DataFrame df = df1.filterColumn(0, (Integer v) -> v > 15);
        new DFAsserts(df, "a")
                .expectHeight(1)
                .expectRow(0, 20);
    }
}
