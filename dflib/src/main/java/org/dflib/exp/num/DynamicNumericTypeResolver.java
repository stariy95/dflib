package org.dflib.exp.num;

import org.dflib.Series;
import org.dflib.builder.ObjectAccum;

import java.math.BigDecimal;
import java.math.BigInteger;

final class DynamicNumericTypeResolver {

    private static final int NO_RANK = Integer.MAX_VALUE;

    private DynamicNumericTypeResolver() {
    }

    static TypeScanResult commonType(Series<?> series) {
        return seriesScan(series);
    }

    static Class<? extends Number> commonType(TypeScanResult one, TypeScanResult two) {
        return typeForRank(Math.min(one.rank(), two.rank()));
    }

    static Class<? extends Number> commonType(TypeScanResult one, TypeScanResult two, TypeScanResult three) {
        return typeForRank(Math.min(one.rank(), Math.min(two.rank(), three.rank())));
    }

    static Class<? extends Number> commonType(Number value) {
        return typeForRank(valueRank(value));
    }

    static Class<? extends Number> commonType(Number one, Number two) {
        return typeForRank(Math.min(valueRank(one), valueRank(two)));
    }

    static Class<? extends Number> commonType(Number one, Number two, Number three) {
        return typeForRank(Math.min(valueRank(one), Math.min(valueRank(two), valueRank(three))));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static ResolvedNumExp<?> resolvedExp(Series<?> series, Class<? extends Number> type, boolean hasNulls) {
        return new ResolvedNumExp(type, convert(series, type, hasNulls));
    }

    static Number castAsNumber(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Can't cast '" + value.getClass().getName() + "' to a number");
        }

        return convert(number, commonType(number));
    }

    static <N extends Number> Series<N> convert(Series<?> series, Class<N> type) {
        return convert(series, type, hasNulls(series));
    }

    @SuppressWarnings("unchecked")
    static <N extends Number> Series<N> convert(Series<?> series, Class<N> type, boolean hasNulls) {
        if (type == Integer.class) {
            return (Series<N>) toIntSeries(series, hasNulls);
        }

        if (type == Long.class) {
            return (Series<N>) toLongSeries(series, hasNulls);
        }

        if (type == Float.class) {
            return (Series<N>) toFloatSeries(series, hasNulls);
        }

        if (type == Double.class) {
            return (Series<N>) toDoubleSeries(series, hasNulls);
        }

        if (type == BigInteger.class) {
            return (Series<N>) toObjectSeries(series, BigInteger.class);
        }

        if (type == BigDecimal.class) {
            return (Series<N>) toObjectSeries(series, BigDecimal.class);
        }

        throw new IllegalArgumentException("Unsupported numeric type: " + type);
    }

    @SuppressWarnings("unchecked")
    static <N extends Number> N convert(Number number, Class<N> type) {
        if (number == null) {
            return null;
        }

        if (type == Integer.class) {
            return (N) Integer.valueOf(number.intValue());
        }

        if (type == Long.class) {
            return (N) Long.valueOf(number.longValue());
        }

        if (type == Float.class) {
            return (N) Float.valueOf(number.floatValue());
        }

        if (type == Double.class) {
            return (N) Double.valueOf(number.doubleValue());
        }

        if (type == BigInteger.class) {
            return (N) toBigInteger(number);
        }

        if (type == BigDecimal.class) {
            return (N) toBigDecimal(number);
        }

        throw new IllegalArgumentException("Unsupported numeric type: " + type);
    }

    private static TypeScanResult seriesScan(Series<?> series) {
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

        return new TypeScanResult(rank, hasNulls);
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
            case 1 -> Double.class;
            case 2 -> Float.class;
            case 3 -> BigInteger.class;
            case 4 -> Long.class;
            case 5 -> Integer.class;
            default -> BigDecimal.class;
        };
    }

    @SuppressWarnings("unchecked")
    private static Series<Integer> toIntSeries(Series<?> series, boolean hasNulls) {
        if (!hasNulls) {
            return ((Series<Number>) series).compactInt(Number::intValue);
        }
        return toObjectSeries(series, Integer.class);
    }

    @SuppressWarnings("unchecked")
    private static Series<Long> toLongSeries(Series<?> series, boolean hasNulls) {
        if (!hasNulls) {
            return ((Series<Number>) series).compactLong(Number::longValue);
        }
        return toObjectSeries(series, Long.class);
    }

    @SuppressWarnings("unchecked")
    private static Series<Float> toFloatSeries(Series<?> series, boolean hasNulls) {
        if (!hasNulls) {
            return ((Series<Number>) series).compactFloat(Number::floatValue);
        }
        return toObjectSeries(series, Float.class);
    }

    @SuppressWarnings("unchecked")
    private static Series<Double> toDoubleSeries(Series<?> series, boolean hasNulls) {
        if (!hasNulls) {
            return ((Series<Number>) series).compactDouble(Number::doubleValue);
        }
        return toObjectSeries(series, Double.class);
    }

    private static <N extends Number> Series<N> toObjectSeries(Series<?> series, Class<N> type) {
        ObjectAccum<N> values = new ObjectAccum<>(series.size());
        for (int i = 0; i < series.size(); i++) {
            values.push(convertValue(series.get(i), type));
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

    private static <N extends Number> N convertValue(Object value, Class<N> type) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Can't cast '" + value.getClass().getName() + "' to a number");
        }

        return convert((Number) value, type);
    }

    private static BigInteger toBigInteger(Number number) {
        if (number instanceof BigInteger) {
            return (BigInteger) number;
        }

        if (number instanceof BigDecimal) {
            return ((BigDecimal) number).toBigInteger();
        }

        return BigInteger.valueOf(number.longValue());
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }

        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }

        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }

        return BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros();
    }
}

record TypeScanResult(int rank, boolean hasNulls) {
    Class<? extends Number> type() {
        return DynamicNumericTypeResolver.typeForRank(rank);
    }
}
