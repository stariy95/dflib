package org.dflib;

/**
 * Represents basic Series statistics.
 *
 * @since 2.0.0
 */
public record SeriesInfo(Class<?> type, Boolean nullable, Integer nullCount, Object min, Object avg, Object max) {

    static final Index INDEX = Index.of("type", "nullable", "null_count", "min", "avg", "max");

    public DataFrame toDataFrame() {
        return DataFrame.foldByRow(INDEX).of(type, nullable, nullCount, min, avg, max);
    }
}
