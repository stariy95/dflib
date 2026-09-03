package org.dflib.ql;

import org.dflib.ql.fn.AbsFunction;
import org.dflib.ql.fn.AvgFunction;
import org.dflib.ql.fn.CastAsBigintFunction;
import org.dflib.ql.fn.CastAsBoolFunction;
import org.dflib.ql.fn.CastAsDateFunction;
import org.dflib.ql.fn.CastAsDateTimeFunction;
import org.dflib.ql.fn.CastAsDecimalFunction;
import org.dflib.ql.fn.CastAsDoubleFunction;
import org.dflib.ql.fn.CastAsFloatFunction;
import org.dflib.ql.fn.CastAsIntFunction;
import org.dflib.ql.fn.CastAsLongFunction;
import org.dflib.ql.fn.CastAsOffsetDateTimeFunction;
import org.dflib.ql.fn.CastAsStrFunction;
import org.dflib.ql.fn.CastAsTimeFunction;
import org.dflib.ql.fn.ConcatFunction;
import org.dflib.ql.fn.ContainsFunction;
import org.dflib.ql.fn.CountFunction;
import org.dflib.ql.fn.CumSumFunction;
import org.dflib.ql.fn.DayFunction;
import org.dflib.ql.fn.EndsWithFunction;
import org.dflib.ql.fn.FirstFunction;
import org.dflib.ql.fn.HourFunction;
import org.dflib.ql.fn.IfFunction;
import org.dflib.ql.fn.IfNullFunction;
import org.dflib.ql.fn.LastFunction;
import org.dflib.ql.fn.LenFunction;
import org.dflib.ql.fn.ListFunction;
import org.dflib.ql.fn.LowerFunction;
import org.dflib.ql.fn.MatchesFunction;
import org.dflib.ql.fn.MaxFunction;
import org.dflib.ql.fn.MedianFunction;
import org.dflib.ql.fn.MillisecondFunction;
import org.dflib.ql.fn.MinFunction;
import org.dflib.ql.fn.MinuteFunction;
import org.dflib.ql.fn.MonthFunction;
import org.dflib.ql.fn.PlusDaysFunction;
import org.dflib.ql.fn.PlusHoursFunction;
import org.dflib.ql.fn.PlusMillisecondsFunction;
import org.dflib.ql.fn.PlusMinutesFunction;
import org.dflib.ql.fn.PlusMonthsFunction;
import org.dflib.ql.fn.PlusNanosFunction;
import org.dflib.ql.fn.PlusSecondsFunction;
import org.dflib.ql.fn.PlusWeeksFunction;
import org.dflib.ql.fn.PlusYearsFunction;
import org.dflib.ql.fn.QuantileFunction;
import org.dflib.ql.fn.RoundFunction;
import org.dflib.ql.fn.RowNumFunction;
import org.dflib.ql.fn.ScaleFunction;
import org.dflib.ql.fn.SecondFunction;
import org.dflib.ql.fn.SetFunction;
import org.dflib.ql.fn.ShiftFunction;
import org.dflib.ql.fn.SplitFunction;
import org.dflib.ql.fn.SqrtFunction;
import org.dflib.ql.fn.StartsWithFunction;
import org.dflib.ql.fn.SubstrFunction;
import org.dflib.ql.fn.SumFunction;
import org.dflib.ql.fn.TrimFunction;
import org.dflib.ql.fn.UpperFunction;
import org.dflib.ql.fn.VConcatFunction;
import org.dflib.ql.fn.YearFunction;

/**
 * Registrations of the built-in QL functions, one {@link QLFunction} class per name in {@code org.dflib.ql.fn}.
 */
class DefaultQLFunctions {

    private DefaultQLFunctions() {
    }

    static void register(QLFunctions.Builder builder) {
        registerStringFunctions(builder);
        registerNumericFunctions(builder);
        registerBooleanFunctions(builder);
        registerCollectionFunctions(builder);
        registerCasts(builder);
        registerFieldFunctions(builder);
        registerTemporalArithmetic(builder);
        registerAggregates(builder);
        registerSpecialForms(builder);
    }

    private static void registerStringFunctions(QLFunctions.Builder builder) {
        builder
                .function("trim", new TrimFunction())
                .function("lower", new LowerFunction())
                .function("upper", new UpperFunction())
                .function("substr", new SubstrFunction())
                .function("concat", new ConcatFunction());
    }

    private static void registerNumericFunctions(QLFunctions.Builder builder) {
        builder
                .function("len", new LenFunction())
                .function("abs", new AbsFunction())
                .function("sqrt", new SqrtFunction())
                .function("round", new RoundFunction())
                .function("rowNum", new RowNumFunction())
                .function("scale", new ScaleFunction())
                .function("count", new CountFunction());
    }

    private static void registerBooleanFunctions(QLFunctions.Builder builder) {
        builder
                .function("castAsBool", new CastAsBoolFunction())
                .function("matches", new MatchesFunction())
                .function("startsWith", new StartsWithFunction())
                .function("endsWith", new EndsWithFunction())
                .function("contains", new ContainsFunction());
    }

    private static void registerCollectionFunctions(QLFunctions.Builder builder) {
        builder
                .function("list", new ListFunction())
                .function("set", new SetFunction())
                .function("split", new SplitFunction());
    }

    private static void registerCasts(QLFunctions.Builder builder) {
        builder
                .function("castAsInt", new CastAsIntFunction())
                .function("castAsLong", new CastAsLongFunction())
                .function("castAsBigint", new CastAsBigintFunction())
                .function("castAsFloat", new CastAsFloatFunction())
                .function("castAsDouble", new CastAsDoubleFunction())
                .function("castAsDecimal", new CastAsDecimalFunction())
                .function("castAsStr", new CastAsStrFunction())
                .function("castAsDate", new CastAsDateFunction())
                .function("castAsTime", new CastAsTimeFunction())
                .function("castAsDateTime", new CastAsDateTimeFunction())
                .function("castAsOffsetDateTime", new CastAsOffsetDateTimeFunction());
    }

    private static void registerFieldFunctions(QLFunctions.Builder builder) {
        builder
                .function("year", new YearFunction())
                .function("month", new MonthFunction())
                .function("day", new DayFunction())
                .function("hour", new HourFunction())
                .function("minute", new MinuteFunction())
                .function("second", new SecondFunction())
                .function("millisecond", new MillisecondFunction());
    }

    private static void registerTemporalArithmetic(QLFunctions.Builder builder) {
        builder
                .function("plusYears", new PlusYearsFunction())
                .function("plusMonths", new PlusMonthsFunction())
                .function("plusWeeks", new PlusWeeksFunction())
                .function("plusDays", new PlusDaysFunction())
                .function("plusHours", new PlusHoursFunction())
                .function("plusMinutes", new PlusMinutesFunction())
                .function("plusSeconds", new PlusSecondsFunction())
                .function("plusMilliseconds", new PlusMillisecondsFunction())
                .function("plusNanos", new PlusNanosFunction());
    }

    private static void registerAggregates(QLFunctions.Builder builder) {
        builder
                .function("min", new MinFunction())
                .function("max", new MaxFunction())
                .function("avg", new AvgFunction())
                .function("median", new MedianFunction())
                .function("quantile", new QuantileFunction())
                .function("sum", new SumFunction())
                .function("cumSum", new CumSumFunction())
                .function("first", new FirstFunction())
                .function("last", new LastFunction())
                .function("vConcat", new VConcatFunction());
    }

    private static void registerSpecialForms(QLFunctions.Builder builder) {
        builder
                .function("if", new IfFunction())
                .function("ifNull", new IfNullFunction())
                .function("shift", new ShiftFunction());
    }
}
