package org.dflib.exp.num;

import org.dflib.DataFrame;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.Series;
import org.dflib.exp.Exp1;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

/**
 * Casts this expression to a numeric type determined at eval time by scanning actual data.
 * Picks the widest numeric type present in the data (e.g., all ints → Integer, mixed → BigDecimal).
 * Throws {@link IllegalArgumentException} if any non-null value is not a {@link Number}.
 *
 * @since 2.0.0
 */
public class CastAsNumExp extends Exp1<Object, Number> implements NumExp<Number> {

    @SuppressWarnings("unchecked")
    public CastAsNumExp(Exp<?> exp) {
        super("castAsNumber", Number.class, (Exp<Object>) exp);
    }

    @Override
    public Series<Number> eval(DataFrame df) {
        return convert(exp.eval(df));
    }

    @Override
    public Series<Number> eval(Series<?> s) {
        return convert(exp.eval(s));
    }

    @Override
    public Number reduce(DataFrame df) {
        return validateNumber(exp.reduce(df));
    }

    @Override
    public Number reduce(Series<?> s) {
        return validateNumber(exp.reduce(s));
    }

    private Number validateNumber(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Number num) {
            return num;
        }
        throw new IllegalArgumentException("Can't cast '" + val.getClass().getName() + "' to a number");
    }

    private Series<Number> convert(Series<?> s) {
        Function<Number, Number> converter = converterForRank(seriesRank(s));
        return s.map(v -> v == null ? null : converter.apply((Number) v));
    }

    private static int seriesRank(Series<?> s) {
        int widestRank = Integer.MAX_VALUE;
        for (int i = 0; i < s.size(); i++) {
            Object val = s.get(i);
            if (val == null) {
                continue;
            }
            if (!(val instanceof Number num)) {
                throw new IllegalArgumentException("Can't cast '" + val.getClass().getName() + "' to a number");
            }
            Integer rank = NumericExpFactory.typeConversionRank.get(num.getClass());
            if(rank == null) {
                widestRank = 0;
            } else if (rank < widestRank) {
                widestRank = rank;
            }
        }
        return widestRank == Integer.MAX_VALUE ? 0 : widestRank;
    }

    /**
     * Must be in sync with the {@link NumericExpFactory#typeConversionRank}
     */
    private static Function<Number, Number> converterForRank(int rank) {
        return switch (rank) {
            case 0 -> n -> {
                if (n instanceof BigDecimal) {
                    return n;
                }
                if (n instanceof BigInteger bi) {
                    return new BigDecimal(bi);
                }
                if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
                    return BigDecimal.valueOf(n.longValue());
                }
                // Float, Double, and other Number subtypes
                return BigDecimal.valueOf(n.doubleValue()).stripTrailingZeros();
            };
            case 1 -> Number::doubleValue;
            case 2 -> Number::floatValue;
            case 3 -> n -> {
                if (n instanceof BigInteger) {
                    return n;
                }
                return BigInteger.valueOf(n.longValue());
            };
            case 4 -> Number::longValue;
            case 5 -> Number::intValue;
            default -> n -> BigDecimal.valueOf(n.doubleValue()).stripTrailingZeros();
        };
    }
}
