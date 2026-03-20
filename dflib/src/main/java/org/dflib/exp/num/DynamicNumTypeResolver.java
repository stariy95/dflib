package org.dflib.exp.num;

import org.dflib.Series;
import org.dflib.builder.ObjectAccum;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.dflib.exp.num.NumericExpFactory.*;

final class DynamicNumTypeResolver {

    private static final int NO_RANK = Integer.MAX_VALUE;

    private DynamicNumTypeResolver() {
    }

    private static ScanResult commonType(Series<?> series) {
        int rank = NO_RANK;
        boolean hasNulls = false;

        for (int i = 0; i < series.size(); i++) {
            Object value = series.get(i);
            if (value == null) {
                hasNulls = true;
            } else {
                int vr = valueRank(value);
                if (vr < rank) {
                    rank = vr;
                }
            }
        }

        return new ScanResult(rank == NO_RANK ? RANK_BIG_DECIMAL : rank, hasNulls);
    }

    static <T> T resolve(Series<?> series, DynamicNumOps.Unary<T> op) {
        ScanResult result = commonType(series);
        int rank = result.rank();
        return op.apply(NumericExpFactory.factory(rank), typeResolvedExp(series, rank, result.hasNulls()));
    }

    static <T> T resolve(Series<?> one, Series<?> two, DynamicNumOps.Binary<T> op) {
        ScanResult result1 = commonType(one);
        ScanResult result2 = commonType(two);
        int rank = Math.min(result1.rank(), result2.rank());
        return op.apply(
                NumericExpFactory.factory(rank),
                typeResolvedExp(one, rank, result1.hasNulls()),
                typeResolvedExp(two, rank, result2.hasNulls())
        );
    }

    static <T> T resolve(Series<?> one, Series<?> two, Series<?> three, DynamicNumOps.Ternary<T> op) {
        ScanResult result1 = commonType(one);
        ScanResult result2 = commonType(two);
        ScanResult result3 = commonType(three);
        int rank = Math.min(result1.rank(), Math.min(result2.rank(), result3.rank()));
        return op.apply(
                NumericExpFactory.factory(rank),
                typeResolvedExp(one, rank, result1.hasNulls()),
                typeResolvedExp(two, rank, result2.hasNulls()),
                typeResolvedExp(three, rank, result3.hasNulls())
        );
    }

    static <T> T resolve(Number value, DynamicNumOps.Unary<T> op) {
        return resolve(Series.ofVal(value, 1), op);
    }

    static <T> T resolve(Number one, Number two, DynamicNumOps.Binary<T> op) {
        return resolve(Series.ofVal(one, 1), Series.ofVal(two, 1), op);
    }

    static <T> T resolve(Number one, Number two, Number three, DynamicNumOps.Ternary<T> op) {
        return resolve(Series.ofVal(one, 1), Series.ofVal(two, 1), Series.ofVal(three, 1), op);
    }

    static <N extends Number> Series<N> convert(Series<?> series, int rank) {
        return convert(series, rank, hasNulls(series));
    }

    @SuppressWarnings("unchecked")
    static <N extends Number> Series<N> convert(Series<?> series, int rank, boolean hasNulls) {
        Series<Number> numSeries = (Series<Number>) series;
        return switch (rank) {
            case RANK_DOUBLE -> (Series<N>) toDoubleSeries(numSeries, hasNulls);
            case RANK_FLOAT -> (Series<N>) toFloatSeries(numSeries, hasNulls);
            case RANK_BIG_INTEGER -> (Series<N>) toObjectSeries(numSeries, RANK_BIG_INTEGER);
            case RANK_LONG -> (Series<N>) toLongSeries(numSeries, hasNulls);
            case RANK_INT -> (Series<N>) toIntSeries(numSeries, hasNulls);
            default -> (Series<N>) toObjectSeries(numSeries, RANK_BIG_DECIMAL);
        };
    }

    // TODO: trace and eliminate this, use value -> series -> resolve as with eval() calls
    static Number convert(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Can't cast '" + value.getClass().getName() + "' to a number");
        }

        // TODO: this does the same null and instanceof check
        int rank = valueRank(number);
        return convert(number, rank);
    }

    @SuppressWarnings("unchecked")
    static <N extends Number> N convert(Number number, int rank) {
        if (number == null) {
            return null;
        }

        return switch (rank) {
            case RANK_DOUBLE -> (N) Double.valueOf(number.doubleValue());
            case RANK_FLOAT -> (N) Float.valueOf(number.floatValue());
            case RANK_BIG_INTEGER -> (N) toBigInteger(number);
            case RANK_LONG -> (N) Long.valueOf(number.longValue());
            case RANK_INT -> (N) Integer.valueOf(number.intValue());
            default -> (N) toBigDecimal(number);
        };
    }

    private static int valueRank(Object value) {
        if (value == null) {
            return NO_RANK;
        }

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Can't cast '" + value.getClass().getName() + "' to a number");
        }

        Integer rank = NumericExpFactory.typeConversionRank.get(value.getClass());
        return rank != null ? rank : 0;
    }

    static Class<? extends Number> typeForRank(int rank) {
        return switch (rank) {
            case RANK_DOUBLE -> Double.class;
            case RANK_FLOAT -> Float.class;
            case RANK_BIG_INTEGER -> BigInteger.class;
            case RANK_LONG -> Long.class;
            case RANK_INT -> Integer.class;
            default -> BigDecimal.class;
        };
    }

    private static Series<Integer> toIntSeries(Series<Number> series, boolean hasNulls) {
        if (!hasNulls) {
            return series.compactInt(Number::intValue);
        }
        return toObjectSeries(series, RANK_INT);
    }

    private static Series<Long> toLongSeries(Series<Number> series, boolean hasNulls) {
        if (!hasNulls) {
            return series.compactLong(Number::longValue);
        }
        return toObjectSeries(series, RANK_LONG);
    }

    private static Series<Float> toFloatSeries(Series<Number> series, boolean hasNulls) {
        if (!hasNulls) {
            return series.compactFloat(Number::floatValue);
        }
        return toObjectSeries(series, RANK_FLOAT);
    }

    private static Series<Double> toDoubleSeries(Series<Number> series, boolean hasNulls) {
        if (!hasNulls) {
            return series.compactDouble(Number::doubleValue);
        }
        return toObjectSeries(series, RANK_DOUBLE);
    }

    private static <N extends Number> Series<N> toObjectSeries(Series<Number> series, int rank) {
        ObjectAccum<N> values = new ObjectAccum<>(series.size());
        for (int i = 0; i < series.size(); i++) {
            values.push(convert(series.get(i), rank));
        }
        return values.toSeries();
    }

    private static boolean hasNulls(Series<?> series) {
        for (int i = 0; i < series.size(); i++) {
            if (series.get(i) == null) {
                return true;
            }
        }
        return false;
    }

    private static BigInteger toBigInteger(Number number) {
        if (number instanceof BigInteger bi) {
            return bi;
        }

        if (number instanceof BigDecimal bd) {
            return bd.toBigInteger();
        }

        return BigInteger.valueOf(number.longValue());
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal bd) {
            return bd;
        }

        if (number instanceof BigInteger bi) {
            return new BigDecimal(bi);
        }

        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }

        return BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros();
    }

    private static ResolvedNumExp<?> typeResolvedExp(Series<?> series, int rank, boolean hasNulls) {
        return new ResolvedNumExp<>(typeForRank(rank), convert(series, rank, hasNulls));
    }

    record ScanResult(int rank, boolean hasNulls) {
    }
}
