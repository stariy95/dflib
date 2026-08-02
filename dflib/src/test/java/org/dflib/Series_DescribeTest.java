package org.dflib;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class Series_DescribeTest {

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void cached(SeriesType type) {
        Series<?> s = type.createSeries(5, 1, 3);
        assertSame(s.describe(), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void empty(SeriesType type) {
        Series<?> s = type.createSeries();
        assertEquals(new SeriesInfo(Object.class, Boolean.TRUE, 0, null, null, null), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void allNulls(SeriesType type) {
        Series<?> s = type.createSeries(null, null);
        assertEquals(new SeriesInfo(Object.class, Boolean.TRUE, 2, null, null, null), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void strings(SeriesType type) {
        Series<?> s = type.createSeries("a", "b", "c");
        assertEquals(new SeriesInfo(String.class, Boolean.TRUE, 0, null, null, null), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void strings_Nulls(SeriesType type) {
        Series<?> s = type.createSeries("a", null, "c", null);
        assertEquals(new SeriesInfo(String.class, Boolean.TRUE, 2, null, null, null), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void ints(SeriesType type) {
        Series<?> s = type.createSeries(5, 1, 3);
        assertEquals(new SeriesInfo(Integer.class, Boolean.TRUE, 0, 1, 3., 5), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void ints_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(5, null, 1);
        assertEquals(new SeriesInfo(Integer.class, Boolean.TRUE, 1, 1, 3., 5), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void longs(SeriesType type) {
        Series<?> s = type.createSeries(5L, 1L, 3L);
        assertEquals(new SeriesInfo(Long.class, Boolean.TRUE, 0, 1L, 3., 5L), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void longs_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(5L, null, 1L);
        assertEquals(new SeriesInfo(Long.class, Boolean.TRUE, 1, 1L, 3., 5L), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void doubles(SeriesType type) {
        Series<?> s = type.createSeries(5.5, 1.5, 3.5);
        assertEquals(new SeriesInfo(Double.class, Boolean.TRUE, 0, 1.5, 3.5, 5.5), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void doubles_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(5.5, null, 1.5);
        assertEquals(new SeriesInfo(Double.class, Boolean.TRUE, 1, 1.5, 3.5, 5.5), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void floats(SeriesType type) {
        Series<?> s = type.createSeries(5.5f, 1.5f, 3.5f);
        assertEquals(new SeriesInfo(Float.class, Boolean.TRUE, 0, 1.5f, 3.5, 5.5f), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void floats_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(5.5f, null, 1.5f);
        assertEquals(new SeriesInfo(Float.class, Boolean.TRUE, 1, 1.5f, 3.5, 5.5f), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void decimals(SeriesType type) {
        Series<?> s = type.createSeries(new BigDecimal("5.5"), new BigDecimal("1.5"), new BigDecimal("3.5"));
        assertEquals(new SeriesInfo(
                BigDecimal.class,
                Boolean.TRUE,
                0,
                new BigDecimal("1.5"),
                new BigDecimal("3.5"),
                new BigDecimal("5.5")), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void decimals_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(new BigDecimal("5.5"), null, new BigDecimal("1.5"));
        assertEquals(new SeriesInfo(
                BigDecimal.class,
                Boolean.TRUE,
                1,
                new BigDecimal("1.5"),
                new BigDecimal("3.5"),
                new BigDecimal("5.5")), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void bigints(SeriesType type) {
        Series<?> s = type.createSeries(new BigInteger("5"), new BigInteger("1"), new BigInteger("3"));
        assertEquals(new SeriesInfo(
                BigInteger.class,
                Boolean.TRUE,
                0,
                new BigInteger("1"),
                new BigDecimal("3"),
                new BigInteger("5")), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void bigints_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(new BigInteger("5"), null, new BigInteger("1"));
        assertEquals(new SeriesInfo(
                BigInteger.class,
                Boolean.TRUE,
                1,
                new BigInteger("1"),
                new BigDecimal("3"),
                new BigInteger("5")), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void dates(SeriesType type) {
        Series<?> s = type.createSeries(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 3));
        assertEquals(new SeriesInfo(
                LocalDate.class,
                Boolean.TRUE,
                0,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 1, 3),
                LocalDate.of(2021, 1, 5)), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void dates_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(LocalDate.of(2021, 1, 5), null, LocalDate.of(2021, 1, 1));
        assertEquals(new SeriesInfo(
                LocalDate.class,
                Boolean.TRUE,
                1,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 1, 3),
                LocalDate.of(2021, 1, 5)), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void dateTimes(SeriesType type) {
        Series<?> s = type.createSeries(
                LocalDateTime.of(2021, 1, 5, 10, 0),
                LocalDateTime.of(2021, 1, 1, 10, 0),
                LocalDateTime.of(2021, 1, 3, 10, 0));

        assertEquals(new SeriesInfo(
                LocalDateTime.class,
                Boolean.TRUE,
                0,
                LocalDateTime.of(2021, 1, 1, 10, 0),
                LocalDateTime.of(2021, 1, 3, 10, 0),
                LocalDateTime.of(2021, 1, 5, 10, 0)), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void dateTimes_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(
                LocalDateTime.of(2021, 1, 5, 10, 0),
                null,
                LocalDateTime.of(2021, 1, 1, 10, 0));

        assertEquals(new SeriesInfo(
                LocalDateTime.class,
                Boolean.TRUE,
                1,
                LocalDateTime.of(2021, 1, 1, 10, 0),
                LocalDateTime.of(2021, 1, 3, 10, 0),
                LocalDateTime.of(2021, 1, 5, 10, 0)), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void times(SeriesType type) {
        Series<?> s = type.createSeries(LocalTime.of(10, 0), LocalTime.of(8, 0), LocalTime.of(9, 0));
        assertEquals(new SeriesInfo(
                LocalTime.class,
                Boolean.TRUE,
                0,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void times_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(LocalTime.of(10, 0), null, LocalTime.of(8, 0));
        assertEquals(new SeriesInfo(
                LocalTime.class,
                Boolean.TRUE,
                1,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)), s.describe());
    }

    // Series of Shorts, Bytes, etc. have no dedicated stats aggregators, and are described as decimals
    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void shorts(SeriesType type) {
        Series<?> s = type.createSeries((short) 5, (short) 1, (short) 3);
        assertEquals(new SeriesInfo(
                Short.class,
                Boolean.TRUE,
                0,
                new BigDecimal("1"),
                new BigDecimal("3"),
                new BigDecimal("5")), s.describe());
    }

    // a Series with mixed number types is described as decimals
    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void mixedNumbers(SeriesType type) {
        Series<?> s = type.createSeries(5L, 1, 3.);
        assertEquals(new SeriesInfo(
                Number.class,
                Boolean.TRUE,
                0,
                new BigDecimal("1"),
                new BigDecimal("3"),
                new BigDecimal("5")), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void mixedNumbers_Nulls(SeriesType type) {
        Series<?> s = type.createSeries(5L, null, 1);
        assertEquals(new SeriesInfo(
                Number.class,
                Boolean.TRUE,
                1,
                new BigDecimal("1"),
                new BigDecimal("3"),
                new BigDecimal("5")), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void mixedNumbers_Fractions(SeriesType type) {
        Series<?> s = type.createSeries(new BigDecimal("1.5"), 2, 3.5f);
        assertEquals(new SeriesInfo(
                Number.class,
                Boolean.TRUE,
                0,
                new BigDecimal("1.5"),
                new BigDecimal("2.33333333333333"),
                new BigDecimal("3.5")), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void noCommonType(SeriesType type) {
        Series<?> s = type.createSeries(5L, "a");
        assertEquals(new SeriesInfo(Object.class, Boolean.TRUE, 0, null, null, null), s.describe());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void boxedBooleans(SeriesType type) {
        Series<?> s = type.createSeries(Boolean.TRUE, null, Boolean.FALSE);
        assertEquals(new SeriesInfo(Boolean.class, Boolean.TRUE, 1, null, null, null), s.describe());
    }
}
