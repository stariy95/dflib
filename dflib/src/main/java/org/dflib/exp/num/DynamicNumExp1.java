package org.dflib.exp.num;

import org.dflib.DataFrame;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.Series;
import org.dflib.exp.Exp1;

class DynamicNumExp1 extends Exp1<Number, Number> implements NumExp<Number> {

    private final DynamicNumOps.Unary op;

    @SuppressWarnings("unchecked")
    DynamicNumExp1(String opName, Exp<? extends Number> exp, DynamicNumOps.Unary op) {
        super(opName, Number.class, (Exp<Number>) exp);
        this.op = op;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Series<Number> eval(DataFrame df) {
        return (Series<Number>) resolve(exp.eval(df)).eval(df);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Series<Number> eval(Series<?> s) {
        return (Series<Number>) resolve(exp.eval(s)).eval(s);
    }

    @Override
    public Number reduce(DataFrame df) {
        return resolve(DynamicNumTypeResolver.convert(exp.reduce(df))).reduce(df);
    }

    @Override
    public Number reduce(Series<?> s) {
        return resolve(DynamicNumTypeResolver.convert(exp.reduce(s))).reduce(s);
    }

    private Exp<? extends Number> resolve(Series<?> series) {
        DynamicNumTypeResolver.TypeScanResult scan = DynamicNumTypeResolver.commonType(series);
        return op.apply(
                NumericExpFactory.factory(scan.rank()),
                DynamicNumTypeResolver.typeResolvedExp(series, scan.rank(), scan.hasNulls())
        );
    }

    private Exp<? extends Number> resolve(Number value) {
        int rank = DynamicNumTypeResolver.commonTypeRank(value);
        return op.apply(
                NumericExpFactory.factory(rank),
                DynamicNumTypeResolver.typeResolvedExp(Series.ofVal(value, 1), rank, false)
        );
    }
}
