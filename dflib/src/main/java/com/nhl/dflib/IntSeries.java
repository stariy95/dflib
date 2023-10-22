package com.nhl.dflib;

import com.nhl.dflib.builder.BoolAccum;
import com.nhl.dflib.builder.IntAccum;
import com.nhl.dflib.series.IntArraySeries;
import com.nhl.dflib.sort.IntComparator;

import java.util.Comparator;
import java.util.Random;

/**
 * A Series optimized to store and access primitive int values without <code>java.lang.Integer</code> wrapper. Can also
 * pose as "Series&lt;Integer>", although this is not the most efficient way of using it.
 *
 * @since 0.6
 */
public interface IntSeries extends Series<Integer> {

    /**
     * @deprecated in favor of {@link Series#ofInt(int...)}
     */
    @Deprecated(since = "0.16", forRemoval = true)
    static IntSeries forInts(int... ints) {
        return Series.ofInt(ints);
    }

    /**
     * @since 0.7
     * @deprecated since 0.16 in favor of {@link #mapAsInt(IntValueMapper)}
     */
    @Deprecated(since = "0.16", forRemoval = true)
    static <V> IntSeries forSeries(Series<V> series, IntValueMapper<? super V> converter) {
        return series.mapAsInt(converter);
    }

    @Override
    default Class<Integer> getNominalType() {
        return Integer.TYPE;
    }

    @Override
    default Class<?> getInferredType() {
        return Integer.TYPE;
    }

    @Override
    default IntSeries castAsInt() {
        return this;
    }

    int getInt(int index);

    void copyToInt(int[] to, int fromOffset, int toOffset, int len);

    IntSeries materializeInt();

    /**
     * @since 0.18
     */
    @Override
    default Series<?> add(Object value) {
        return value instanceof Integer
                ? addInt((Integer) value)
                : Series.super.add(value);
    }

    /**
     * Creates a new Series with a provided int appended to the end of this Series.
     *
     * @since 0.18
     */
    default IntSeries addInt(int val) {
        int s = size();

        int[] data = new int[s + 1];
        this.copyToInt(data, 0, 0, s);
        data[s] = val;
        return new IntArraySeries(data);
    }

    IntSeries concatInt(IntSeries... other);

    IntSeries rangeOpenClosedInt(int fromInclusive, int toExclusive);

    @Override
    IntSeries head(int len);

    /**
     * @deprecated since 0.18 in favor of {@link #head(int)} that returns an IntSeries
     */
    @Deprecated(since = "0.18", forRemoval = true)
    default IntSeries headInt(int len) {
        return head(len);
    }

    @Override
    IntSeries tail(int len);

    /**
     * @deprecated since 0.18 in favor of {@link #tail(int)} that returns an IntSeries
     */
    @Deprecated(since = "0.18", forRemoval = true)
    default IntSeries tailInt(int len) {
        return tail(len);
    }

    /**
     * @since 0.11
     */
    IntSeries selectInt(Condition condition);

    /**
     * @since 0.11
     */
    IntSeries selectInt(IntPredicate p);

    /**
     * @since 0.11
     */
    IntSeries selectInt(BooleanSeries positions);

    @Override
    IntSeries sort(Sorter... sorters);

    @Override
    IntSeries sort(Comparator<? super Integer> comparator);

    IntSeries sortInt();

    IntSeries sortInt(IntComparator comparator);

    /**
     * @since 0.8
     */
    IntSeries sortIndexInt();

    /**
     * @since 0.8
     */
    IntSeries sortIndexInt(IntComparator comparator);

    /**
     * Returns an IntSeries that represents positions in the Series that match the predicate. The returned value can be
     * used to "select" data from this Series or from DataFrame containing this Series. Same as {@link #index(ValuePredicate)},
     * only usually much faster.
     *
     * @param predicate match condition
     * @return an IntSeries that represents positions in the Series that match the predicate. Negative values denote
     * null values.
     */
    IntSeries indexInt(IntPredicate predicate);

    BooleanSeries locateInt(IntPredicate predicate);

    @Override
    IntSeries unique();

    /**
     * @deprecated in favor of {@link #unique()}
     */
    @Deprecated(since = "0.18", forRemoval = true)
    default IntSeries uniqueInt() {
        return unique();
    }

    /**
     * @since 0.7
     */
    @Override
    IntSeries sample(int size);

    /**
     * @since 0.7
     */
    @Override
    IntSeries sample(int size, Random random);

    /**
     * @since 0.7
     */
    default int[] toIntArray() {
        int len = size();
        int[] copy = new int[len];
        copyToInt(copy, 0, 0, len);
        return copy;
    }

    /**
     * @since 0.14
     */
    LongSeries cumSum();

    /**
     * @since 0.7
     */
    int max();

    /**
     * @since 0.7
     */
    int min();

    /**
     * @since 0.7
     */
    long sum();

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
    default IntSeries add(IntSeries s) {
        int len = size();
        IntAccum accumulator = new IntAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushInt(this.getInt(i) + s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * Performs subtraction operation between this and another IntSeries.
     *
     * @since 0.11
     */
    default IntSeries sub(IntSeries s) {
        int len = size();
        IntAccum accumulator = new IntAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushInt(this.getInt(i) - s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * Performs multiplication operation between this and another IntSeries.
     *
     * @since 0.11
     */
    default IntSeries mul(IntSeries s) {
        int len = size();
        IntAccum accumulator = new IntAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushInt(this.getInt(i) * s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * Performs division operation between this and another IntSeries.
     *
     * @since 0.11
     */
    default IntSeries div(IntSeries s) {
        int len = size();
        IntAccum accumulator = new IntAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushInt(this.getInt(i) / s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * Performs modulo operation between this and another IntSeries.
     *
     * @since 0.11
     */
    default IntSeries mod(IntSeries s) {
        int len = size();
        IntAccum accumulator = new IntAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushInt(this.getInt(i) % s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries lt(IntSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getInt(i) < s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries le(IntSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getInt(i) <= s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries gt(IntSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getInt(i) > s.getInt(i));
        }

        return accumulator.toSeries();
    }

    /**
     * @since 0.11
     */
    default BooleanSeries ge(IntSeries s) {
        int len = size();
        BoolAccum accumulator = new BoolAccum(len);

        for (int i = 0; i < len; i++) {
            accumulator.pushBool(this.getInt(i) >= s.getInt(i));
        }

        return accumulator.toSeries();
    }
}
