package org.dflib;

import java.util.Arrays;

/**
 * Represents basic DataFrame statistics.
 *
 * @since 2.0.0
 */
public record DataFrameInfo(ColumnInfo[] columns) {

    public DataFrame toDataFrame() {

        int h = columns.length;

        int[] indices = new int[h];
        String[] names = new String[h];
        Class<?>[] types = new Class[h];
        Boolean[] nullables = new Boolean[h];
        Integer[] nullCounts = new Integer[h];
        Object[] mins = new Object[h];
        Object[] avgs = new Object[h];
        Object[] maxs = new Object[h];

        for (int i = 0; i < h; i++) {
            ColumnInfo ci = columns[i];

            indices[i] = ci.index();
            names[i] = ci.name();
            types[i] = ci.type();
            nullables[i] = ci.nullable();
            nullCounts[i] = ci.nullCount();
            mins[i] = ci.min();
            avgs[i] = ci.avg();
            maxs[i] = ci.max();
        }

        return DataFrame.byColumn(ColumnInfo.INDEX).of(
                Series.ofInt(indices),
                Series.of(names),
                Series.of(types),
                Series.of(nullables),
                Series.of(nullCounts),
                Series.of(mins),
                Series.of(avgs),
                Series.of(maxs));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof DataFrameInfo that && Arrays.equals(columns, that.columns));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(columns);
    }

    @Override
    public String toString() {
        return "DataFrameInfo" + Arrays.toString(columns);
    }
}
