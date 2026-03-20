package org.dflib.exp.num;

import org.dflib.DataFrame;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.Series;
import org.dflib.exp.Exp1;

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
        Series<?> series = exp.eval(df);
        return convert(series);
    }

    @Override
    public Series<Number> eval(Series<?> s) {
        Series<?> series = exp.eval(s);
        return convert(series);
    }

    @Override
    public Number reduce(DataFrame df) {
        return DynamicNumericTypeResolver.castAsNumber(exp.reduce(df));
    }

    @Override
    public Number reduce(Series<?> s) {
        return DynamicNumericTypeResolver.castAsNumber(exp.reduce(s));
    }

    @SuppressWarnings("unchecked")
    private Series<Number> convert(Series<?> s) {
        TypeScanResult scan = DynamicNumericTypeResolver.commonType(s);
        return (Series<Number>) DynamicNumericTypeResolver.convert(s, scan.type(), scan.hasNulls());
    }
}
