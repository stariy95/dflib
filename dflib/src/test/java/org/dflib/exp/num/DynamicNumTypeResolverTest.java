package org.dflib.exp.num;

import org.dflib.Series;
import org.dflib.series.ObjectSeries;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DynamicNumTypeResolverTest {

    @Test
    public void resolve_trustedNominalSeries_skipsScan() {
        CountingSeries delegate = new CountingSeries(Series.of(new BigDecimal("1.5"), new BigDecimal("2.5")));
        Series<? extends Number> trusted = new ResolvedNominalSeries<>(BigDecimal.class, delegate, false);

        Series<?> resolved = DynamicNumTypeResolver.resolve(trusted, (factory, exp) -> exp.eval(Series.ofVal(null, 2)));

        assertEquals(2, resolved.size());
        assertEquals(0, delegate.getCount);
    }

    @Test
    public void resolve_unknownSeries_scansBeforeEval() {
        CountingSeries delegate = new CountingSeries(Series.of(new BigDecimal("1.5"), new BigDecimal("2.5")));

        Series<?> resolved = DynamicNumTypeResolver.resolve(delegate, (factory, exp) -> exp.eval(Series.ofVal(null, 2)));

        assertEquals(2, resolved.size());
        assertEquals(4, delegate.getCount);
    }

    @Test
    public void resolve_convertedObjectSeries_returnsTrustedWrapper() {
        Series<?> resolved = DynamicNumTypeResolver.resolve(
                Series.of(1, new BigDecimal("2.5")),
                (factory, exp) -> exp.eval(Series.ofVal(null, 2)));

        assertSame(BigDecimal.class, resolved.getNominalType());
    }

    static class CountingSeries extends ObjectSeries<Number> {

        private final Series<? extends Number> delegate;
        int getCount;

        CountingSeries(Series<? extends Number> delegate) {
            super(Object.class);
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public Number get(int index) {
            getCount++;
            return delegate.get(index);
        }

        @Override
        public void copyTo(Object[] to, int fromOffset, int toOffset, int len) {
            for (int i = 0; i < len; i++) {
                to[toOffset + i] = get(fromOffset + i);
            }
        }

        @Override
        public Series<Number> materialize() {
            return this;
        }

        @Override
        public Series<Number> fillNulls(Number value) {
            return delegate.unsafeCastAs(Number.class).fillNulls(value);
        }

        @Override
        public Series<Number> fillNullsFromSeries(Series<? extends Number> values) {
            return delegate.unsafeCastAs(Number.class).fillNullsFromSeries(values);
        }

        @Override
        public Series<Number> fillNullsBackwards() {
            return delegate.unsafeCastAs(Number.class).fillNullsBackwards();
        }

        @Override
        public Series<Number> fillNullsForward() {
            return delegate.unsafeCastAs(Number.class).fillNullsForward();
        }
    }
}
