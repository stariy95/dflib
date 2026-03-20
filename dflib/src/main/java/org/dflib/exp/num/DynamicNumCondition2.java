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
                DynamicNumericTypeResolver.castAsNumber(left.reduce(df)),
                DynamicNumericTypeResolver.castAsNumber(right.reduce(df))).reduce(df);
    }

    @Override
    public Boolean reduce(Series<?> s) {
        return resolve(
                DynamicNumericTypeResolver.castAsNumber(left.reduce(s)),
                DynamicNumericTypeResolver.castAsNumber(right.reduce(s))
        ).reduce(s);
    }

    private Condition resolve(Series<?> leftSeries, Series<?> rightSeries) {
        TypeScanResult scanL = DynamicNumericTypeResolver.commonType(leftSeries);
        TypeScanResult scanR = DynamicNumericTypeResolver.commonType(rightSeries);
        Class<? extends Number> type = DynamicNumericTypeResolver.commonType(scanL, scanR);
        return op.apply(
                NumericExpFactory.factory(type),
                DynamicNumericTypeResolver.resolvedExp(leftSeries, type, scanL.hasNulls()),
                DynamicNumericTypeResolver.resolvedExp(rightSeries, type, scanR.hasNulls())
        );
    }

    private Condition resolve(Number leftValue, Number rightValue) {
        Class<? extends Number> type = DynamicNumericTypeResolver.commonType(leftValue, rightValue);
        return op.apply(
                NumericExpFactory.factory(type),
                DynamicNumericTypeResolver.resolvedExp(Series.ofVal(leftValue, 1), type, false),
                DynamicNumericTypeResolver.resolvedExp(Series.ofVal(rightValue, 1), type, false)
        );
    }
}
