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
 * Registrations of the QL built-in functions. This class is a pure registration list: every built-in is a
 * {@link QLFunction} class in {@code org.dflib.ql.fn}, one per name, whose typed {@code call} overloads javac checks
 * against the expression interfaces they declare.
 * <p>
 * The declared signature and the producer must agree: for any arguments matching the signature, the classifier of
 * the produced expression must be the descriptor's effective return type. The parser casts the result to the type
 * the signature promised, so a mismatch is a {@code ClassCastException} inside generated code. Declaring the exact
 * {@code Exp} subinterface a {@code call} overload returns is what makes this a compile-time property rather than a
 * convention. It is verified by {@code DefaultQLFunctionsTest}.
 * <p>
 * There is no shared "temporal" or "aggregate" expression interface in DFLib - {@code year()}, {@code plusDays()}
 * and {@code min()} are declared separately on {@code DateExp}, {@code TimeExp} and friends - so a function that
 * accepts several receiver types declares one {@code call} overload per receiver, and the receivers it does not
 * support are simply the ones it declares no overload for.
 */
class DefaultQLFunctions {

    private DefaultQLFunctions() {
    }

    static QLFunctions.Builder register(QLFunctions.Builder builder) {
        builder = registerStringFunctions(builder);
        builder = registerNumericFunctions(builder);
        builder = registerBooleanFunctions(builder);
        builder = registerCollectionFunctions(builder);
        builder = registerCasts(builder);
        builder = registerFieldFunctions(builder);
        builder = registerTemporalArithmetic(builder);
        builder = registerAggregates(builder);
        builder = registerSpecialForms(builder);
        return builder;
    }

    private static QLFunctions.Builder registerStringFunctions(QLFunctions.Builder builder) {
        return builder
                .function("trim", new TrimFunction())
                .function("lower", new LowerFunction())
                .function("upper", new UpperFunction())
                .function("substr", new SubstrFunction())
                .function("concat", new ConcatFunction());
    }

    private static QLFunctions.Builder registerNumericFunctions(QLFunctions.Builder builder) {
        return builder
                .function("len", new LenFunction())
                .function("abs", new AbsFunction())
                .function("sqrt", new SqrtFunction())
                .function("round", new RoundFunction())
                .function("rowNum", new RowNumFunction())
                .function("scale", new ScaleFunction())
                .function("count", new CountFunction());
    }

    private static QLFunctions.Builder registerBooleanFunctions(QLFunctions.Builder builder) {
        return builder
                .function("castAsBool", new CastAsBoolFunction())
                .function("matches", new MatchesFunction())
                .function("startsWith", new StartsWithFunction())
                .function("endsWith", new EndsWithFunction())
                .function("contains", new ContainsFunction());
    }

    /**
     * Collection- and array-valued functions. None of these has a dedicated expression type, so they are all plain
     * OBJECT.
     */
    private static QLFunctions.Builder registerCollectionFunctions(QLFunctions.Builder builder) {
        return builder
                .function("list", new ListFunction())
                .function("set", new SetFunction())
                .function("split", new SplitFunction());
    }

    /**
     * Casts. Every cast has a fixed return type and accepts an expression of any type, mirroring the grammar, where
     * the cast argument is an untyped {@code expression}. Temporal casts additionally accept a constant format
     * string.
     */
    private static QLFunctions.Builder registerCasts(QLFunctions.Builder builder) {
        return builder
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

    /**
     * Temporal field accessors. All of them return an int regardless of the receiver, so their return type is fixed
     * and only the set of receivers they accept differs: "year"/"month"/"day" have no time receiver, and
     * "hour".."millisecond" have no date one.
     */
    private static QLFunctions.Builder registerFieldFunctions(QLFunctions.Builder builder) {
        return builder
                .function("year", new YearFunction())
                .function("month", new MonthFunction())
                .function("day", new DayFunction())
                .function("hour", new HourFunction())
                .function("minute", new MinuteFunction())
                .function("second", new SecondFunction())
                .function("millisecond", new MillisecondFunction());
    }

    /**
     * "plusX" functions. Each returns the receiver's own type, so a name that has more than one receiver is
     * polymorphic even though every one of its overloads has a fixed return.
     */
    private static QLFunctions.Builder registerTemporalArithmetic(QLFunctions.Builder builder) {
        return builder
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

    private static QLFunctions.Builder registerAggregates(QLFunctions.Builder builder) {
        return builder

                // "min"/"max"/"avg"/"median"/"quantile" preserve the receiver type, so they are polymorphic across
                // their receiver overloads. "sum"/"cumSum" are numeric-only, and have a fixed NUMERIC return: a
                // fixed return keeps their call sites single-alternative in the grammar
                .function("min", new MinFunction())
                .function("max", new MaxFunction())
                .function("avg", new AvgFunction())
                .function("median", new MedianFunction())
                .function("quantile", new QuantileFunction())

                .function("sum", new SumFunction())
                .function("cumSum", new CumSumFunction())

                // "first"/"last"/"vConcat" have no typed expression implementation - they produce an expression
                // whose value type is only recoverable at eval time - so they are declared as returning a bare
                // "Exp", reachable only from the untyped expression position
                .function("first", new FirstFunction())
                .function("last", new LastFunction())
                .function("vConcat", new VConcatFunction());
    }

    private static QLFunctions.Builder registerSpecialForms(QLFunctions.Builder builder) {
        return builder

                // "if" and "ifNull" produce IfExp/IfNullExp, which implement none of the typed Exp interfaces
                .function("if", new IfFunction())
                .function("ifNull", new IfNullFunction())

                // "shift" is covariantly overridden on every typed expression interface except Condition, so it
                // returns the receiver's own type - except for a boolean receiver, where it produces a plain
                // Exp<Boolean> rather than a Condition
                .function("shift", new ShiftFunction());
    }
}
