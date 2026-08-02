package org.dflib;

import org.dflib.unit.DataFrameAsserts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SeriesInfoTest {

    @Test
    public void toDataFrame_Primitive() {
        DataFrame df = Series.ofInt(5, 1, 3).describe().toDataFrame();
        new DataFrameAsserts(df, "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(1)
                .expectRow(0, Integer.TYPE, false, null, 1., 3., 5.);
    }

    @Test
    public void toDataFrame_Object() {
        DataFrame df = Series.of(5, null, 1).describe().toDataFrame();
        new DataFrameAsserts(df, "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(1)
                .expectRow(0, Integer.class, true, 1, 1, 3., 5);
    }

    @Test
    public void toDataFrame_NoStats() {
        DataFrame df = Series.of("a", "b").describe().toDataFrame();
        new DataFrameAsserts(df, "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(1)
                .expectRow(0, String.class, true, 0, null, null, null);
    }

    @Test
    public void toDataFrame_Date() {
        DataFrame df = Series.of(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 5)).describe().toDataFrame();
        new DataFrameAsserts(df, "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(1)
                .expectRow(0, LocalDate.class, true, 0, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 3), LocalDate.of(2021, 1, 5));
    }

    @Test
    public void toDataFrame_Decimal() {
        DataFrame df = Series.of(new BigDecimal("1.5"), new BigDecimal("3.5")).describe().toDataFrame();
        new DataFrameAsserts(df, "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(1)
                .expectRow(0, BigDecimal.class, true, 0, new BigDecimal("1.5"), new BigDecimal("2.5"), new BigDecimal("3.5"));
    }
}
