package com.nhl.dflib;

import com.nhl.dflib.builder.BoolAccum;
import com.nhl.dflib.builder.DoubleAccum;
import com.nhl.dflib.series.DoubleArraySeries;

import java.util.Comparator;
import java.util.Random;

/**
 * A Series optimized to store and access primitive double values without <code>java.lang.Double</code> wrapper. Can also
 * pose as "Series&lt;Double>", although this is not the most efficient way of using it.
 *
 * @since 0.6
 */
public interface DoubleSeries extends Series<Double> {

    /**
     * @deprecated in favor of {@link Series#ofDouble(double...)}
     */
    @Deprecated(since = "0.16", forRemoval = true)
    static DoubleSeries forDoubles(double... doubles) {
        return Series.ofDouble(doubles);
    }

    /**
     * @since 0.7
     * @deprecated in favor of {@link #mapAsDouble(DoubleValueMapper)}
     */
    @Deprecated(since = "0.16", forRemoval = true)
    static <V> DoubleSeries forSeries(Series<V> series, DoubleValueMapper<? super V> converter) {
        return series.mapAsDouble(converter);
    }

    @Override
    default Class<Double> getNominalType() {
        return Double.TYPE;
    }

    @Override
    default Class<?> getInferredType() {
        return Double.TYPE;
    }

    @Override
    default DoubleSeries castAsDouble() {
        return this;
    }

    double getDouble(int index);

    void copyToDouble(double[] to, int fromOffset, int toOffset, int len);

    DoubleSeries materializeDouble();

    /**
     * @since 0.18
     */
    @Override
    default Series<?> add(Object value) {
        return value instanceof Double
                ? addDouble((Double) value)
                : Series.super.add(value);
    }

    /**
     * Creates a new Series with a provided int appended to the end of this Series.
     *
     * @since 0.18
     */
    default DoubleSeries addDouble(double val) {
        int s = size();

        double[] data = new double[s + 1];
        this.copyToDouble(data, 0, 0, s);
        data[s] = val;
        return new DoubleArraySeries(data);
    }

    DoubleSeries concatDouble(DoubleSeries... other);

    DoubleSeries rangeOpenClosedDouble(int fromInclusive, int toExclusive);

    @Override
    DoubleSeries head(int len);

    /**
     * @deprecated in favor of {@link #head(int)}
     */
    @Deprecated(since = "0.18", forRemoval = true)
    default DoubleSeries headDouble(int len) {
        return head(len);
    }

    @Override
    DoubleSeries tail(int len);

    /**
     * @deprecated in favor of {@link #tail(int)}
     */
    @Deprecated(since = "0.18", forRemoval = true)
    default DoubleSeries tailDouble(int len) {
        return tail(len);
    }

    /**
     * @since 0.11
     */
    DoubleSeries selectDouble(Condition condition);

    /**
     * @since 0.11
     */
    DoubleSeries selectDouble(DoublePredicate p);

    /**
     * @since 0.11
     */
    DoubleSeries selectDouble(BooleanSeries positions);

    @Override
    DoubleSeries sort(Sorter... sorters);

    @Override
    DoubleSeries sort(Comparator<? super Double> comparator);

    DoubleSeries sortDouble();

    // TODO: implement 'sortDouble(DoubleComparator)' similar to how IntBaseSeries does "sortInt(IntComparator)"

    /**
     * Returns an IntSeries that represents positions in the Series that match the predicate. The returned value can be
     * used to "select" data from this Series or from DataFrame containing this Series. Same as {@link #index(ValuePredicate)},
     * only usually much faster.
     *
     * @param predicate match condition
     * @return an IntSeries that represents positions in the Series that match the predicate.
     */
    IntSeries indexDouble(DoublePredicate predicate);

    BooleanSeries locateDouble(DoublePredicate predicate);

    @Override
    DoubleSeries unique();

    /**
     * @deprecated in favor of {@link #unique()}
     */
    @Deprecated(since = "0.18", forRemoval = true)
    default DoubleSeries uniqueDouble() {
        return unique();
    }

    /**
     * @since 0.7
     */
    @Override
    DoubleSeries sample(int size);

    /**
     * @since 0.7
     */
    @Override
    DoubleSeries sample(int size, Random random);

    /**
     * @since 0.7
     */
    default double[] toDoubleArray() {
        int len = size();
        double[] copy = new double[len];
        copyToDouble(copy, 0, 0, len);
        return copy;
    }

    /**
     * @since 0.14
     */
    DoubleSeries cumSum();

    /**
     * @since 0.7
     */
    double max();

    /**
     * @since 0.7
     */
    double min();

    /**
     * @since 0.7
     */
    // TODO: deal with overflow?
    double sum();

    /**
     * @since 0.11
     */
    double avg();

    /**
     * @since 0.7
     */
    double median();

    /**
     * @since 0.11
     */
    default DoubleSeries add(DoubleSeries s) {
        int len = size();
        DoubleAccum accumulator = new DoubleAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushDouble(this.getDouble(i) + s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * Performs subtraction operation between this and another DoubleSeries.
     *
     * @since 0.11
     */
    default DoubleSeries sub(DoubleSeries s) {
        int len = size();
        DoubleAccum accumulator = new DoubleAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushDouble(this.getDouble(i) - s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * Performs multiplication operation between this and another DoubleSeries.
     *
     * @since 0.11
     */
    default DoubleSeries mul(DoubleSeries s) {
        int len = size();
        DoubleAccum accumulator = new DoubleAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushDouble(this.getDouble(i) * s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * Performs division operation between this and another DoubleSeries.
     *
     * @since 0.11
     */
    default DoubleSeries div(DoubleSeries s) {
        int len = size();
        DoubleAccum accumulator = new DoubleAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushDouble(this.getDouble(i) / s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default DoubleSeries mod(DoubleSeries s) {
        int len = size();
        DoubleAccum accumulator = new DoubleAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushDouble(this.getDouble(i) % s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries lt(DoubleSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getDouble(i) < s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries le(DoubleSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getDouble(i) <= s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries gt(DoubleSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getDouble(i) > s.getDouble(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries ge(DoubleSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getDouble(i) >= s.getDouble(i));
        }

        return accumulator.toSeries();
    }
}
