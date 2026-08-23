package org.dflib.junit5;

import org.dflib.DataFrame;
import org.dflib.Index;

import java.util.List;

/**
 * @deprecated in favor of {@link org.dflib.junit.DataFrameAsserts}.
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class DataFrameAsserts extends org.dflib.junit.DataFrameAsserts {

    public DataFrameAsserts(DataFrame df, Index expectedColumns) {
        super(df, expectedColumns);
    }

    public DataFrameAsserts(DataFrame df, List<String> expectedColumns) {
        super(df, expectedColumns);
    }

    public DataFrameAsserts(DataFrame df, String... expectedColumns) {
        super(df, expectedColumns);
    }

    @Override
    public DataFrameAsserts expectHeight(int expectedHeight) {
        super.expectHeight(expectedHeight);
        return this;
    }

    @Override
    public DataFrameAsserts expectIntColumns(int... positions) {
        super.expectIntColumns(positions);
        return this;
    }

    @Override
    public DataFrameAsserts expectIntColumns(String... labels) {
        super.expectIntColumns(labels);
        return this;
    }

    @Override
    public DataFrameAsserts expectLongColumns(int... positions) {
        super.expectLongColumns(positions);
        return this;
    }

    @Override
    public DataFrameAsserts expectLongColumns(String... labels) {
        super.expectLongColumns(labels);
        return this;
    }

    @Override
    public DataFrameAsserts expectDoubleColumns(int... positions) {
        super.expectDoubleColumns(positions);
        return this;
    }

    @Override
    public DataFrameAsserts expectDoubleColumns(String... labels) {
        super.expectDoubleColumns(labels);
        return this;
    }

    @Override
    public DataFrameAsserts expectBooleanColumns(int... positions) {
        super.expectBooleanColumns(positions);
        return this;
    }

    @Override
    public DataFrameAsserts expectBooleanColumns(String... labels) {
        super.expectBooleanColumns(labels);
        return this;
    }

    @Override
    public DataFrameAsserts expectColumn(int pos, Object... expectedValues) {
        super.expectColumn(pos, expectedValues);
        return this;
    }

    @Override
    public DataFrameAsserts expectColumn(String column, Object... expectedValues) {
        super.expectColumn(column, expectedValues);
        return this;
    }

    @Override
    public DataFrameAsserts expectRow(int pos, Object... expectedValues) {
        super.expectRow(pos, expectedValues);
        return this;
    }
}
