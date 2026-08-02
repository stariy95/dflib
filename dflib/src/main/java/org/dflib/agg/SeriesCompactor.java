package org.dflib.agg;

import org.dflib.Condition;
import org.dflib.DoubleSeries;
import org.dflib.Exp;
import org.dflib.FloatSeries;
import org.dflib.IntSeries;
import org.dflib.LongSeries;
import org.dflib.Series;

import java.math.BigDecimal;
import java.math.BigInteger;

class SeriesCompactor {

    private static final Condition notNullExp = Exp.$col(0).isNotNull();

    public static IntSeries toIntSeries(Series<? extends Number> s) {
        return (s instanceof IntSeries)
                ? (IntSeries) s
                : s.select(notNullExp).compactInt(Number::intValue);
    }

    public static LongSeries toLongSeries(Series<? extends Number> s) {
        return (s instanceof LongSeries)
                ? (LongSeries) s
                : s.select(notNullExp).compactLong(Number::longValue);
    }

    public static FloatSeries toFloatSeries(Series<? extends Number> s) {
        return (s instanceof FloatSeries)
                ? (FloatSeries) s
                : s.select(notNullExp).compactFloat(Number::floatValue);
    }

    public static DoubleSeries toDoubleSeries(Series<? extends Number> s) {
        return (s instanceof DoubleSeries)
                ? (DoubleSeries) s
                : s.select(notNullExp).compactDouble(Number::doubleValue);
    }

    /**
     * Converts a Series of arbitrary Numbers to a Series of BigDecimals, skipping nulls.
     */
    public static Series<BigDecimal> toDecimalSeries(Series<? extends Number> s) {
        return noNullsSeries(s).map(SeriesCompactor::toDecimal);
    }

    private static BigDecimal toDecimal(Number n) {
        return switch (n) {
            case BigDecimal bd -> bd;
            case BigInteger bi -> new BigDecimal(bi);
            case Integer i -> BigDecimal.valueOf(i);
            case Long l -> BigDecimal.valueOf(l);
            case Short sh -> BigDecimal.valueOf(sh);
            case Byte b -> BigDecimal.valueOf(b);

            // going through "Float.toString(..)" instead of "doubleValue()", as the latter would turn 2.2f
            // into 2.200000047683716
            case Float f -> new BigDecimal(Float.toString(f));

            // "BigDecimal.valueOf(double)" is slower than "new BigDecimal(double)", but is preferable, as it
            // deals with double precision correctly
            default -> BigDecimal.valueOf(n.doubleValue());
        };
    }

    public static <T> Series<T> noNullsSeries(Series<T> s) {
        return s.select(notNullExp);
    }
}
