package org.dflib;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataFrame_DescribeTest {

    @Test
    public void empty() {
        DataFrameInfo info = DataFrame.empty("a", "b").describe();
        assertEquals(new DataFrameInfo(new ColumnInfo[]{
                new ColumnInfo(0, "a", Object.class, Boolean.TRUE, 0, null, null, null),
                new ColumnInfo(1, "b", Object.class, Boolean.TRUE, 0, null, null, null)
        }), info);
    }

    @Test
    public void noColumns() {
        DataFrameInfo info = DataFrame.empty().describe();
        assertArrayEquals(new ColumnInfo[0], info.columns());
    }

    @Test
    public void primitiveColumn() {
        DataFrameInfo info = DataFrame
                .byColumn("a")
                .of(Series.ofInt(5, 1, 3))
                .describe();

        assertEquals(new DataFrameInfo(new ColumnInfo[]{
                new ColumnInfo(0, "a", Integer.TYPE, Boolean.FALSE, null, 1., 3., 5.)
        }), info);
    }

    @Test
    public void mixedColumns() {
        DataFrameInfo info = DataFrame
                .byColumn("i", "s", "d")
                .of(
                        Series.of(5, null, 1),
                        Series.of("a", "b", "c"),
                        Series.of(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 3)))
                .describe();

        assertEquals(new DataFrameInfo(new ColumnInfo[]{
                new ColumnInfo(0, "i", Integer.class, Boolean.TRUE, 1, 1, 3., 5),
                new ColumnInfo(1, "s", String.class, Boolean.TRUE, 0, null, null, null),
                new ColumnInfo(2, "d", LocalDate.class, Boolean.TRUE, 0,
                        LocalDate.of(2021, 1, 1),
                        LocalDate.of(2021, 1, 3),
                        LocalDate.of(2021, 1, 5))
        }), info);
    }

    @Test
    public void columnInfoMatchesSeriesInfo() {
        Series<?> s = Series.of(5.5, null, 1.5);
        ColumnInfo ci = DataFrame.byColumn("a").of(s).describe().columns()[0];
        SeriesInfo si = s.describe();

        assertEquals(new ColumnInfo(0, "a", si.type(), si.nullable(), si.nullCount(), si.min(), si.avg(), si.max()), ci);
    }
}
