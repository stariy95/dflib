package org.dflib;

/**
 * Represents basic statistics for a Series column within a DataFrame
 *
 * @since 2.0.0
 */
public record ColumnInfo(
        int index,
        String name,
        Class<?> type,
        Boolean nullable,
        Integer nullCount,
        Object min,
        Object avg,
        Object max) {

    static final Index INDEX = Index.of("index", "name", "type", "nullable", "null_count", "min", "avg", "max");

    static ColumnInfo of(int index, String name, SeriesInfo info) {
        return new ColumnInfo(index, name, info.type(), info.nullable(), info.nullCount(), info.min(), info.avg(), info.max());
    }

    public DataFrame toDataFrame() {
        return DataFrame.foldByRow(INDEX).of(index, name, type, nullable, nullCount, min, avg, max);
    }
}
