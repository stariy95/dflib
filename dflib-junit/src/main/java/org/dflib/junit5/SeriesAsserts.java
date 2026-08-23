package org.dflib.junit5;

import org.dflib.Series;

/**
 * @deprecated in favor of {@link org.dflib.junit.SeriesAsserts}.
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class SeriesAsserts extends org.dflib.junit.SeriesAsserts {

    public SeriesAsserts(Series<?> series) {
        super(series);
    }

    @Override
    public SeriesAsserts expectData(Object... expectedValues) {
        super.expectData(expectedValues);
        return this;
    }
}
