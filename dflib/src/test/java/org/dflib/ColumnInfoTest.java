package org.dflib;

import org.dflib.unit.DataFrameAsserts;
import org.junit.jupiter.api.Test;

public class ColumnInfoTest {

    @Test
    public void toDataFrame() {
        DataFrame df = new ColumnInfo(3, "a", Integer.TYPE, Boolean.FALSE, null, 1., 3., 5.).toDataFrame();
        new DataFrameAsserts(df, "index", "name", "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(1)
                .expectRow(0, 3, "a", Integer.TYPE, false, null, 1., 3., 5.);
    }

    @Test
    public void toDataFrame_NoStats() {
        DataFrame df = new ColumnInfo(0, "a", String.class, Boolean.TRUE, 2, null, null, null).toDataFrame();
        new DataFrameAsserts(df, "index", "name", "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(1)
                .expectRow(0, 0, "a", String.class, true, 2, null, null, null);
    }
}
