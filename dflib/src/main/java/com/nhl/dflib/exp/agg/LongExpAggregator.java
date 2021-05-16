package com.nhl.dflib.exp.agg;

import com.nhl.dflib.DataFrame;
import com.nhl.dflib.NumExp;
import com.nhl.dflib.Series;
import com.nhl.dflib.Exp;
import com.nhl.dflib.series.SingleValueSeries;

import java.util.function.Function;

/**
 * @since 0.11
 */
public class LongExpAggregator<S> implements NumExp<Long> {

    private final Exp<S> exp;
    private final Function<Series<S>, Long> aggregator;

    public LongExpAggregator(Exp<S> exp, Function<Series<S>, Long> aggregator) {
        this.exp = exp;
        this.aggregator = aggregator;
    }

    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public Series<Long> eval(DataFrame df) {

        // TODO: optimize for primitive series.
        //  E.g. "IntSeries.average()" is faster than "AggregatorFunctions.averageDouble()"

        long val = aggregator.apply(exp.eval(df));
        return new SingleValueSeries<>(val, 1);
    }

    @Override
    public Series<Long> eval(Series<?> s) {
        // TODO: optimize for primitive series.
        //  E.g. "IntSeries.average()" is faster than "AggregatorFunctions.averageDouble()"

        long val = aggregator.apply(exp.eval(s));
        return new SingleValueSeries<>(val, 1);
    }

    @Override
    public String getName() {
        return exp.getName();
    }

    @Override
    public String getName(DataFrame df) {
        return exp.getName(df);
    }
}
