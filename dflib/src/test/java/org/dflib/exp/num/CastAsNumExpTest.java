package org.dflib.exp.num;

import org.dflib.DataFrame;
import org.dflib.NumExp;
import org.dflib.Series;
import org.dflib.unit.SeriesAsserts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.dflib.Exp.$col;
import static org.dflib.Exp.$int;
import static org.junit.jupiter.api.Assertions.*;

public class CastAsNumExpTest {

    @Test
    public void castAsNumber_add() {
        NumExp<?> exp = $col(0).castAsNumber().add(5);
        Series<Object> s = Series.of(new BigDecimal("5.01"), null, 12L, 1);
        new SeriesAsserts(exp.eval(s))
                .expectData(new BigDecimal("10.01"), null, new BigDecimal("17"), new BigDecimal("6"));
    }

    @Test
    public void castAsNumber_mul() {
        NumExp<?> exp = $col(0).castAsNumber().mul(2);
        Series<Object> s = Series.of(new BigDecimal("5.01"), null, 12L, 1);
        new SeriesAsserts(exp.eval(s))
                .expectData(new BigDecimal("10.02"), null, new BigDecimal("24"), new BigDecimal("2"));
    }

    @Test
    public void castAsNumber_homogeneous() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1, null, 2, 3);
        new SeriesAsserts(exp.eval(s))
                .expectData(1, null, 2, 3);
    }

    @Test
    public void castAsNumber_nonNumeric() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of("abc", 1, 2);
        assertThrows(IllegalArgumentException.class, () -> exp.eval(s));
    }

    // --- Homogeneous type inference ---

    @Test
    public void homogeneous_Long() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1L, null, 2L, 3L);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1L, null, 2L, 3L);
    }

    @Test
    public void homogeneous_Double() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1.0, null, 2.0);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1.0, null, 2.0);
    }

    @Test
    public void homogeneous_Float() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1.0f, null, 2.0f);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1.0f, null, 2.0f);
    }

    @Test
    public void homogeneous_BigInteger() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(BigInteger.ONE, null, BigInteger.TEN);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(BigInteger.ONE, null, BigInteger.TEN);
    }

    @Test
    public void homogeneous_BigDecimal() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(new BigDecimal("1.5"), null);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(new BigDecimal("1.5"), null);
    }

    // --- Pairwise widening ---

    @Test
    public void widen_Int_Long() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1, 2L);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1L, 2L);
    }

    @Test
    public void widen_Int_Double() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1, 2.0);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1.0, 2.0);
    }

    @Test
    public void widen_Int_Float() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1, 2.0f);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1.0f, 2.0f);
    }

    @Test
    public void widen_Int_BigInteger() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1, BigInteger.TEN);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(BigInteger.valueOf(1), BigInteger.TEN);
    }

    @Test
    public void widen_Long_Double() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1L, 2.0);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1.0, 2.0);
    }

    @Test
    public void widen_Long_BigInteger() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1L, BigInteger.TEN);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(BigInteger.valueOf(1), BigInteger.TEN);
    }

    @Test
    public void widen_Float_Double() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(1.0f, 2.0);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(1.0, 2.0);
    }

    @Test
    public void widen_BigInteger_BigDecimal() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(BigInteger.ONE, new BigDecimal("2.5"));
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(new BigDecimal(BigInteger.ONE), new BigDecimal("2.5"));
    }

    // --- Edge cases ---

    @Test
    public void emptySeries() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of();
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData();
    }

    @Test
    public void allNulls() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(null, null, null);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(null, null, null);
    }

    @Test
    public void unknownNumberSubtype() {
        // Custom Number subclass not in typeConversionRank → falls back to BigDecimal via doubleValue()
        Number custom = new Number() {
            @Override
            public int intValue() {
                return 42;
            }

            @Override
            public long longValue() {
                return 42L;
            }

            @Override
            public float floatValue() {
                return 42.0f;
            }

            @Override
            public double doubleValue() {
                return 42.0;
            }
        };

        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(custom, 10);
        Series<? extends Number> result = exp.eval(s);

        // unknown subtype forces BigDecimal; both values converted via BigDecimal path
        new SeriesAsserts(result).expectData(new BigDecimal("42"), BigDecimal.valueOf(10));
    }

    // --- DataFrame eval path ---

    @Test
    public void eval_DataFrame() {
        NumExp<?> exp = $col("a").castAsNumber();
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2L, "y",
                new BigDecimal("3.5"), "z");
        Series<? extends Number> result = exp.eval(df);
        new SeriesAsserts(result).expectData(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3.5"));
    }

    // --- reduce() methods ---

    @Test
    public void reduce_Series_number() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(42, 1, 2);
        assertEquals(42, exp.reduce(s));
    }

    @Test
    public void reduce_Series_null() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(null, 1, 2);
        assertNull(exp.reduce(s));
    }

    @Test
    public void reduce_Series_nonNumeric() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of("abc", 1, 2);
        assertThrows(IllegalArgumentException.class, () -> exp.reduce(s));
    }

    @Test
    public void reduce_DataFrame() {
        NumExp<?> exp = $col("a").castAsNumber();
        DataFrame df = DataFrame.foldByRow("a", "b").of(
                99, "x",
                2, "y");
        assertEquals(99, exp.reduce(df));
    }

    // --- Converter specifics ---

    @Test
    public void bigDecimalConversion_fromBigInteger() {
        NumExp<?> exp = $col(0).castAsNumber();
        Series<Object> s = Series.of(BigInteger.valueOf(123), new BigDecimal("4.5"));
        Series<? extends Number> result = exp.eval(s);
        // BigInteger→BigDecimal uses new BigDecimal(BigInteger)
        new SeriesAsserts(result).expectData(new BigDecimal(BigInteger.valueOf(123)), new BigDecimal("4.5"));
    }

    @Test
    public void bigDecimalConversion_fromDouble() {
        NumExp<?> exp = $col(0).castAsNumber();
        // Mix Double and BigDecimal to trigger BigDecimal target with Double conversion
        Series<Object> s = Series.of(1.5, new BigDecimal("2.5"));
        Series<? extends Number> result = exp.eval(s);
        // Double→BigDecimal uses BigDecimal.valueOf(doubleValue()).stripTrailingZeros()
        new SeriesAsserts(result).expectData(new BigDecimal("1.5"), new BigDecimal("2.5"));
    }

    @Test
    public void bigDecimalConversion_fromLong() {
        NumExp<?> exp = $col(0).castAsNumber();
        // Mix Long and BigDecimal to trigger BigDecimal target with Long→longValue() conversion
        Series<Object> s = Series.of(100L, new BigDecimal("2.5"));
        Series<? extends Number> result = exp.eval(s);
        // Long→BigDecimal uses BigDecimal.valueOf(longValue()), not stripTrailingZeros()
        new SeriesAsserts(result).expectData(new BigDecimal("100"), new BigDecimal("2.5"));
    }

    // --- Short/Byte fallback to BigDecimal ---

    @Test
    public void widen_Short_toBigDecimal() {
        NumExp<?> exp = $col(0).castAsNumber();
        // Short is not in typeConversionRank → triggers BigDecimal fallback
        // Short is converted via longValue() path, not doubleValue()
        Series<Object> s = Series.of((short) 1, 2);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(BigDecimal.valueOf(1), BigDecimal.valueOf(2));
    }

    @Test
    public void widen_Byte_toBigDecimal() {
        NumExp<?> exp = $col(0).castAsNumber();
        // Byte is not in typeConversionRank → triggers BigDecimal fallback
        // Byte is converted via longValue() path, not doubleValue()
        Series<Object> s = Series.of((byte) 1, 2);
        Series<? extends Number> result = exp.eval(s);
        new SeriesAsserts(result).expectData(BigDecimal.valueOf(1), BigDecimal.valueOf(2));
    }

    // --- NumExp identity override ---

    @Test
    public void castAsNumber_alreadyNumeric() {
        NumExp<?> exp = $int(0);
        assertSame(exp, exp.castAsNumber());
    }

    // --- toQL serialization ---

    @Test
    public void toQL() {
        String ql = $col("a").castAsNumber().toQL();
        assertEquals("castAsNum(a)", ql);
    }
}
