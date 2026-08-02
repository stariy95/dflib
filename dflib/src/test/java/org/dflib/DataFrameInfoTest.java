package org.dflib;

import org.dflib.unit.DataFrameAsserts;
import org.junit.jupiter.api.Test;

public class DataFrameInfoTest {

    @Test
    public void toDataFrame() {
        DataFrame df = DataFrame
                .byColumn("i", "s")
                .of(
                        Series.ofInt(5, 1, 3),
                        Series.of("a", null, "c"))
                .describe()
                .toDataFrame();

        new DataFrameAsserts(df, "index", "name", "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(2)
                .expectRow(0, 0, "i", Integer.TYPE, false, null, 1., 3., 5.)
                .expectRow(1, 1, "s", String.class, true, 1, null, null, null);
    }

    @Test
    public void toDataFrame_NoColumns() {
        DataFrame df = DataFrame.empty().describe().toDataFrame();
        new DataFrameAsserts(df, "index", "name", "type", "nullable", "null_count", "min", "avg", "max")
                .expectHeight(0);
    }
}
