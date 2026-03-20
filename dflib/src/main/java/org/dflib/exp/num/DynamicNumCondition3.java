package org.dflib.exp.num;

import org.dflib.BooleanSeries;
import org.dflib.Condition;
import org.dflib.DataFrame;
import org.dflib.Exp;
import org.dflib.Series;
import org.dflib.exp.Exp3;

class DynamicNumCondition3 extends Exp3<Number, Number, Number, Boolean> implements Condition {

    private final DynamicNumOps.TernaryCondition op;

    @SuppressWarnings("unchecked")
    DynamicNumCondition3(
            String opName1,
            String opName2,
            Exp<? extends Number> one,
            Exp<? extends Number> two,
            Exp<? extends Number> three,
            DynamicNumOps.TernaryCondition op) {
        super(opName1, opName2, Boolean.class, (Exp<Number>) one, (Exp<Number>) two, (Exp<Number>) three);
        this.op = op;
    }

    @Override
    public BooleanSeries eval(DataFrame df) {
        return resolve(one.eval(df), two.eval(df), three.eval(df)).eval(df);
    }

    @Override
    public BooleanSeries eval(Series<?> s) {
        return resolve(one.eval(s), two.eval(s), three.eval(s)).eval(s);
    }

    @Override
    public Boolean reduce(DataFrame df) {
        return resolve(
                DynamicNumericTypeResolver.castAsNumber(one.reduce(df)),
                DynamicNumericTypeResolver.castAsNumber(two.reduce(df)),
                DynamicNumericTypeResolver.castAsNumber(three.reduce(df))).reduce(df);
    }

    @Override
    public Boolean reduce(Series<?> s) {
        return resolve(
                DynamicNumericTypeResolver.castAsNumber(one.reduce(s)),
                DynamicNumericTypeResolver.castAsNumber(two.reduce(s)),
                DynamicNumericTypeResolver.castAsNumber(three.reduce(s))).reduce(s);
    }

    private Condition resolve(Series<?> oneSeries, Series<?> twoSeries, Series<?> threeSeries) {
        TypeScanResult scan1 = DynamicNumericTypeResolver.commonType(oneSeries);
        TypeScanResult scan2 = DynamicNumericTypeResolver.commonType(twoSeries);
        TypeScanResult scan3 = DynamicNumericTypeResolver.commonType(threeSeries);
        Class<? extends Number> type = DynamicNumericTypeResolver.commonType(scan1, scan2, scan3);
        return op.apply(
                NumericExpFactory.factory(type),
                DynamicNumericTypeResolver.resolvedExp(oneSeries, type, scan1.hasNulls()),
                DynamicNumericTypeResolver.resolvedExp(twoSeries, type, scan2.hasNulls()),
                DynamicNumericTypeResolver.resolvedExp(threeSeries, type, scan3.hasNulls())
        );
    }

    private Condition resolve(Number oneValue, Number twoValue, Number threeValue) {
        Class<? extends Number> type = DynamicNumericTypeResolver.commonType(oneValue, twoValue, threeValue);
        return op.apply(
                NumericExpFactory.factory(type),
                DynamicNumericTypeResolver.resolvedExp(Series.ofVal(oneValue, 1), type, false),
                DynamicNumericTypeResolver.resolvedExp(Series.ofVal(twoValue, 1), type, false),
                DynamicNumericTypeResolver.resolvedExp(Series.ofVal(threeValue, 1), type, false)
        );
    }
}
