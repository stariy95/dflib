package org.dflib.exp.num;

import org.dflib.DataFrame;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.Series;
import org.dflib.exp.Exp2;

class DynamicNumExp2 extends Exp2<Number, Number, Number> implements NumExp<Number> {

    private final DynamicNumOps.Binary op;

    @SuppressWarnings("unchecked")
    DynamicNumExp2(String opName, Exp<? extends Number> left, Exp<? extends Number> right, DynamicNumOps.Binary op) {
        super(opName, Number.class, (Exp<Number>) left, (Exp<Number>) right);
        this.op = op;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Series<Number> eval(DataFrame df) {
        return (Series<Number>) resolve(left.eval(df), right.eval(df)).eval(df);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Series<Number> eval(Series<?> s) {
        return (Series<Number>) resolve(left.eval(s), right.eval(s)).eval(s);
    }

    @Override
    public Number reduce(DataFrame df) {
        return resolve(
                DynamicNumTypeResolver.convert(left.reduce(df)),
                DynamicNumTypeResolver.convert(right.reduce(df))
        ).reduce(df);
    }

    @Override
    public Number reduce(Series<?> s) {
        return resolve(
                DynamicNumTypeResolver.convert(left.reduce(s)),
                DynamicNumTypeResolver.convert(right.reduce(s))
        ).reduce(s);
    }

    private Exp<? extends Number> resolve(Series<?> leftSeries, Series<?> rightSeries) {
        DynamicNumTypeResolver.TypeScanResult scanL = DynamicNumTypeResolver.commonType(leftSeries);
        DynamicNumTypeResolver.TypeScanResult scanR = DynamicNumTypeResolver.commonType(rightSeries);
        int rank = DynamicNumTypeResolver.commonTypeRank(scanL, scanR);
        return op.apply(
                NumericExpFactory.factory(rank),
                DynamicNumTypeResolver.typeResolvedExp(leftSeries, rank, scanL.hasNulls()),
                DynamicNumTypeResolver.typeResolvedExp(rightSeries, rank, scanR.hasNulls())
        );
    }

    private Exp<? extends Number> resolve(Number leftValue, Number rightValue) {
        int rank = DynamicNumTypeResolver.commonTypeRank(leftValue, rightValue);
        return op.apply(
                NumericExpFactory.factory(rank),
                DynamicNumTypeResolver.typeResolvedExp(Series.ofVal(leftValue, 1), rank, false),
                DynamicNumTypeResolver.typeResolvedExp(Series.ofVal(rightValue, 1), rank, false)
        );
    }
}
