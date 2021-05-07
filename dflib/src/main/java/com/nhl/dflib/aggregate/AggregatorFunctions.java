package com.nhl.dflib.aggregate;

import com.nhl.dflib.Series;
import com.nhl.dflib.ValueMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class AggregatorFunctions {

    /**
     * @since 0.11
     */
    public static <T> Function<Series<T>, T> first() {
        return s -> s.size() == 0 ? null : s.get(0);
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Double> averageDouble() {
        return fromCollector(Collectors.averagingDouble(v -> v.doubleValue()));
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Double> medianDouble() {
        return fromCollector(AggregatorFunctions.medianCollector());
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Integer> sumInt() {
        return fromCollector(Collectors.summingInt(v -> v.intValue()));
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Long> sumLong() {
        return fromCollector(Collectors.summingLong(v -> v.longValue()));
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Double> sumDouble() {
        return fromCollector(Collectors.summingDouble(v -> v.doubleValue()));
    }

    /**
     * @since 0.11
     */
    public static Function<Series<BigDecimal>, BigDecimal> sumDecimal() {
        return s -> BigDecimalSeriesSum.sum(s);
    }

    /**
     * @since 0.11
     */
    public static Function<Series<BigDecimal>, BigDecimal> sumDecimal(int resultScale, RoundingMode resultRoundingMode) {
        return s -> BigDecimalSeriesSum.sum(s, resultScale, resultRoundingMode);
    }

    /**
     * @since 0.11
     */
    public static <S extends Comparable<S>> Function<Series<S>, S> min() {
        return SeriesMinMax::min;
    }

    /**
     * @since 0.11
     */
    public static <S extends Comparable<S>> Function<Series<S>, S> max() {
        return SeriesMinMax::max;
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Integer> minInt() {
        return SeriesMinMax::minInt;
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Integer> maxInt() {
        return SeriesMinMax::maxInt;
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Long> minLong() {
        return SeriesMinMax::minLong;
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Long> maxLong() {
        return SeriesMinMax::maxLong;
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Double> minDouble() {
        return SeriesMinMax::minDouble;
    }

    /**
     * @since 0.11
     */
    public static <S extends Number> Function<Series<S>, Double> maxDouble() {
        return SeriesMinMax::maxDouble;
    }

    /**
     * @since 0.11
     */
    public static <S> Function<Series<S>, String> concat(String delimiter) {
        return fromCollector(mappedCollector(Collectors.joining(delimiter), String::valueOf));
    }

    /**
     * @since 0.11
     */
    public static <S> Function<Series<S>, String> concat(String delimiter, String prefix, String suffix) {
        return fromCollector(mappedCollector(Collectors.joining(delimiter, prefix, suffix), String::valueOf));
    }

    /**
     * @since 0.11
     */
    public static <S, A, T> Function<Series<S>, T> fromCollector(Collector<S, A, T> collector) {

        return s -> {
            BiConsumer accumulator = collector.accumulator();
            A accumResult = collector.supplier().get();

            int len = s.size();
            for (int i = 0; i < len; i++) {
                accumulator.accept(accumResult, s.get(i));
            }

            return collector.finisher().apply(accumResult);
        };
    }

    public static <S, M, A, T> Collector<S, A, T> mappedCollector(Collector<M, A, T> collector, ValueMapper<S, M> mapper) {

        return new Collector<S, A, T>() {
            @Override
            public Supplier<A> supplier() {
                return collector.supplier();
            }

            @Override
            public BiConsumer<A, S> accumulator() {
                BiConsumer<A, M> accum = collector.accumulator();
                return (a, s) -> accum.accept(a, mapper.map(s));
            }

            @Override
            public BinaryOperator<A> combiner() {
                return collector.combiner();
            }

            @Override
            public Function<A, T> finisher() {
                return collector.finisher();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return collector.characteristics();
            }
        };
    }

    public static <S extends Number> Collector<S, List<Double>, Double> medianCollector() {
        return new AggregationCollector<>(
                ArrayList::new,
                (list, d) -> {
                    if (d != null) {
                        list.add(d.doubleValue());
                    }
                },
                medianWithSideEffects());
    }

    private static Function<List<Double>, Double> medianWithSideEffects() {
        return list -> {

            if (list.isEmpty()) {
                return 0.;
            }

            switch (list.size()) {
                case 0:
                    return 0.;
                case 1:
                    return list.get(0);
                default:
                    // side effect - resorting the list; since the list is not exposed outside of "medianCollector", this should
                    // not cause any issues
                    Collections.sort(list);

                    int m = list.size() / 2;

                    int odd = list.size() % 2;
                    if (odd == 1) {
                        return list.get(m);
                    }

                    double d1 = list.get(m - 1);
                    double d2 = list.get(m);
                    return d1 + (d2 - d1) / 2.;
            }
        };
    }
}
