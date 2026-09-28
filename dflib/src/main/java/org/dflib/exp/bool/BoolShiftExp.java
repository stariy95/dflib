package org.dflib.exp.bool;

import org.dflib.BooleanSeries;
import org.dflib.Condition;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.dflib.builder.BoolBuilder;

import java.util.Objects;

/**
 * A shift of a {@link Condition}. As a {@link BooleanSeries} can't store nulls, the head or tail gaps produced by
 * the shift are filled with the filler value, which is "false" unless specified otherwise.
 *
 * @since 2.0.0
 */
public class BoolShiftExp implements Condition {

    private final Condition delegate;
    private final int offset;
    private final boolean filler;

    public BoolShiftExp(Condition delegate, int offset, boolean filler) {
        this.delegate = Objects.requireNonNull(delegate);
        this.offset = offset;
        this.filler = filler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BoolShiftExp that = (BoolShiftExp) o;
        return offset == that.offset
                && filler == that.filler
                && Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, offset, filler);
    }

    @Override
    public String toString() {
        return toQL();
    }

    @Override
    public String toQL() {
        return "shift(" + delegate.toQL() + "," + offset + "," + filler + ")";
    }

    @Override
    public String toQL(DataFrame df) {
        return "shift(" + delegate.toQL(df) + "," + offset + "," + filler + ")";
    }

    @Override
    public BooleanSeries eval(DataFrame df) {
        return shift(delegate.eval(df));
    }

    @Override
    public BooleanSeries eval(Series<?> s) {
        return shift(delegate.eval(s));
    }

    @Override
    public Boolean reduce(DataFrame df) {
        // TODO: what's a meaningful "reduce" for "shift()"?
        throw new UnsupportedOperationException("Shift expression does not define a 'reduce' operation");
    }

    @Override
    public Boolean reduce(Series<?> s) {
        // TODO: what's a meaningful "reduce" for "shift()"?
        throw new UnsupportedOperationException("Shift expression does not define a 'reduce' operation");
    }

    private BooleanSeries shift(BooleanSeries s) {
        int len = s.size();
        return BoolBuilder.buildSeries(i -> {
            long from = (long) i - offset;
            return from >= 0 && from < len ? s.getBool((int) from) : filler;
        }, len);
    }
}
