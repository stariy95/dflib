package org.dflib.exp.num;

import org.dflib.BooleanSeries;
import org.dflib.Condition;
import org.dflib.DataFrame;
import org.dflib.Exp;
import org.dflib.Series;
import org.dflib.exp.Exp2;

class DynamicNumCondition2 extends Exp2<Number, Number, Boolean> implements Condition {

    private final DynamicNumOps.BinaryCondition op;

    @SuppressWarnings("unchecked")
    DynamicNumCondition2(String opName, Exp<? extends Number> left, Exp<? extends Number> right, DynamicNumOps.BinaryCondition op) {
        super(opName, Boolean.class, (Exp<Number>) left, (Exp<Number>) right);
        this.op = op;
    }

    @Override
    public BooleanSeries eval(DataFrame df) {
        return resolve(left.eval(df), right.eval(df)).eval(df);
    }

    @Override
    public BooleanSeries eval(Series<?> s) {
        return resolve(left.eval(s), right.eval(s)).eval(s);
    }

    @Override
    public Boolean reduce(DataFrame df) {
        return resolve(
                DynamicNumTypeResolver.convert(left.reduce(df)),
                DynamicNumTypeResolver.convert(right.reduce(df))
        ).reduce(df);
    }

    @Override
    public Boolean reduce(Series<?> s) {
        return resolve(
                DynamicNumTypeResolver.convert(left.reduce(s)),
                DynamicNumTypeResolver.convert(right.reduce(s))
        ).reduce(s);
    }

    private Condition resolve(Series<?> leftSeries, Series<?> rightSeries) {
        DynamicNumTypeResolver.TypeScanResult scanL = DynamicNumTypeResolver.commonType(leftSeries);
        DynamicNumTypeResolver.TypeScanResult scanR = DynamicNumTypeResolver.commonType(rightSeries);
        int rank = DynamicNumTypeResolver.commonTypeRank(scanL, scanR);
        return op.apply(
                NumericExpFactory.factory(rank),
                DynamicNumTypeResolver.typeResolvedExp(leftSeries, rank, scanL.hasNulls()),
                DynamicNumTypeResolver.typeResolvedExp(rightSeries, rank, scanR.hasNulls())
        );
    }

    private Condition resolve(Number leftValue, Number rightValue) {
        int rank = DynamicNumTypeResolver.commonTypeRank(leftValue, rightValue);
        return op.apply(
                NumericExpFactory.factory(rank),
                DynamicNumTypeResolver.typeResolvedExp(Series.ofVal(leftValue, 1), rank, false),
                DynamicNumTypeResolver.typeResolvedExp(Series.ofVal(rightValue, 1), rank, false)
        );
    }
}
