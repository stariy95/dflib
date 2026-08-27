package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.Series;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.exp.ScalarExp;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import java.math.BigInteger;

import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.ANY;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.BOOLEAN;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.DATE;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.DATETIME;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NUMERIC;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.OBJECT;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.OFFSETDATETIME;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.STRING;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.TIME;
import static org.dflib.ql.QLFunctionSignature.signature;

/**
 * Registrations of the QL built-in functions. Each function is declared as an explicit
 * {@link QLFunctionSignature} over a lambda producer, rather than a reflectively described {@code Udf} class.
 * <p>
 * The declared signature and the producer must agree: for any arguments matching the signature, the classifier of
 * the produced expression must be the descriptor's effective return type. The parser casts the result to the type
 * the signature promised, so a mismatch is a {@code ClassCastException} inside generated code. This is verified by
 * {@code DefaultQLFunctionsTest}.
 * <p>
 * There is no shared "temporal" or "aggregate" expression interface in DFLib - {@code year()}, {@code plusDays()}
 * and {@code min()} are declared separately on {@code DateExp}, {@code TimeExp} and friends - so a function that
 * accepts several receiver types is registered once with an {@code OBJECT} receiver and dispatches on the receiver
 * interface inside its producer (see {@link #field}, {@link #plus}, {@link #agg1}, {@link #quantile},
 * {@link #shift}). Receiver types a function does not support are rejected by {@link #unsupported} at parse time.
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

                .function("trim", signature()
                        .returning(STRING)
                        .arg(OBJECT)
                        .as(args -> args.get(0).trim()))

                .function("lower", signature()
                        .returning(STRING)
                        .arg(OBJECT)
                        .as(args -> args.get(0).lower()))

                .function("upper", signature()
                        .returning(STRING)
                        .arg(OBJECT)
                        .as(args -> args.get(0).upper()))

                .function("substr", signature()
                        .returning(STRING)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0).substr(constantInt(args.get(1)))))

                .function("substr", signature()
                        .returning(STRING)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0).substr(constantInt(args.get(1)), constantInt(args.get(2)))))

                // "concat()" with no arguments is a separate fixed-arity overload beside the vararg one. Both
                // produce the same expression, but the fixed one documents the arity and is preferred by the
                // resolver, which never has to fall back to varargs for an empty call
                .function("concat", signature()
                        .returning(STRING)
                        .as(args -> Exp.concat()))

                .function("concat", signature()
                        .returning(STRING)
                        .varArgs()
                        // arguments are passed as expressions, not as evaluated values: "concat" accepts any mix
                        .as(args -> Exp.concat(args.toArray())));
    }

    private static QLFunctions.Builder registerNumericFunctions(QLFunctions.Builder builder) {
        return builder

                .function("len", signature()
                        .returning(NUMERIC)
                        .arg(STRING)
                        .as(args -> args.get(0).castAsStr().len()))

                .function("abs", signature()
                        .returning(NUMERIC)
                        .arg(NUMERIC)
                        .as(args -> num("abs", args.get(0)).abs()))

                .function("sqrt", signature()
                        .returning(NUMERIC)
                        .arg(NUMERIC)
                        .as(args -> num("sqrt", args.get(0)).sqrt()))

                .function("round", signature()
                        .returning(NUMERIC)
                        .arg(NUMERIC)
                        .as(args -> num("round", args.get(0)).round()))

                .function("rowNum", signature()
                        .returning(NUMERIC)
                        .as(args -> Exp.rowNum()))

                .function("scale", signature()
                        .returning(NUMERIC)
                        .arg(NUMERIC)
                        .constArg(NUMERIC)
                        .as(args -> num("scale", args.get(0)).castAsDecimal().scale(constantInt(args.get(1)))))

                .function("count", signature()
                        .returning(NUMERIC)
                        .as(args -> Exp.count()))

                .function("count", signature()
                        .returning(NUMERIC)
                        .arg(BOOLEAN)
                        .as(args -> Exp.count(cond("count", args.get(0)))));
    }

    private static QLFunctions.Builder registerBooleanFunctions(QLFunctions.Builder builder) {
        return builder

                .function("castAsBool", signature()
                        .returning(BOOLEAN)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsBool()))

                .function("matches", signature()
                        .returning(BOOLEAN)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).matches(constantString(args.get(1)))))

                .function("startsWith", signature()
                        .returning(BOOLEAN)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).startsWith(constantString(args.get(1)))))

                .function("endsWith", signature()
                        .returning(BOOLEAN)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).endsWith(constantString(args.get(1)))))

                .function("contains", signature()
                        .returning(BOOLEAN)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).contains(constantString(args.get(1)))));
    }

    /**
     * Collection- and array-valued functions. None of these has a dedicated expression type, so they are all plain
     * OBJECT.
     */
    private static QLFunctions.Builder registerCollectionFunctions(QLFunctions.Builder builder) {
        return builder

                .function("list", signature()
                        .returning(OBJECT)
                        .arg(OBJECT)
                        .as(args -> args.get(0).list()))

                .function("set", signature()
                        .returning(OBJECT)
                        .arg(OBJECT)
                        .as(args -> args.get(0).set()))

                .function("split", signature()
                        .returning(OBJECT)
                        .arg(STRING)
                        .constArg(STRING)
                        .as(args -> args.get(0).castAsStr().split(constantString(args.get(1)))))

                .function("split", signature()
                        .returning(OBJECT)
                        .arg(STRING)
                        .constArg(STRING)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0).castAsStr()
                                .split(constantString(args.get(1)), constantInt(args.get(2)))));
    }

    /**
     * Casts. Every cast has a fixed return type and accepts an expression of any type, mirroring the grammar, where
     * the cast argument is an untyped {@code expression}. Temporal casts additionally accept a constant format
     * string.
     */
    private static QLFunctions.Builder registerCasts(QLFunctions.Builder builder) {
        return builder

                .function("castAsInt", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsInt()))

                .function("castAsLong", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsLong()))

                .function("castAsBigint", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsBigint()))

                .function("castAsFloat", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsFloat()))

                .function("castAsDouble", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsDouble()))

                .function("castAsDecimal", signature()
                        .returning(NUMERIC)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsDecimal()))

                .function("castAsStr", signature()
                        .returning(STRING)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsStr()))

                .function("castAsDate", signature()
                        .returning(DATE)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsDate()))

                .function("castAsDate", signature()
                        .returning(DATE)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).castAsDate(constantString(args.get(1)))))

                .function("castAsTime", signature()
                        .returning(TIME)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsTime()))

                .function("castAsTime", signature()
                        .returning(TIME)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).castAsTime(constantString(args.get(1)))))

                .function("castAsDateTime", signature()
                        .returning(DATETIME)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsDateTime()))

                .function("castAsDateTime", signature()
                        .returning(DATETIME)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).castAsDateTime(constantString(args.get(1)))))

                .function("castAsOffsetDateTime", signature()
                        .returning(OFFSETDATETIME)
                        .arg(OBJECT)
                        .as(args -> args.get(0).castAsOffsetDateTime()))

                .function("castAsOffsetDateTime", signature()
                        .returning(OFFSETDATETIME)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).castAsOffsetDateTime(constantString(args.get(1)))));
    }

    /**
     * Temporal field accessors. All of them return an int regardless of the receiver, so their return type is fixed
     * and only the set of receivers they accept differs (see {@link #field}).
     */
    private static QLFunctions.Builder registerFieldFunctions(QLFunctions.Builder builder) {
        return builder
                .function("year", fieldFn("year"))
                .function("month", fieldFn("month"))
                .function("day", fieldFn("day"))
                .function("hour", fieldFn("hour"))
                .function("minute", fieldFn("minute"))
                .function("second", fieldFn("second"))
                .function("millisecond", fieldFn("millisecond"));
    }

    /**
     * "plusX" functions. These return the receiver's own type, so they are polymorphic in argument 0.
     */
    private static QLFunctions.Builder registerTemporalArithmetic(QLFunctions.Builder builder) {
        return builder
                .function("plusYears", plusFn("plusYears"))
                .function("plusMonths", plusFn("plusMonths"))
                .function("plusWeeks", plusFn("plusWeeks"))
                .function("plusDays", plusFn("plusDays"))
                .function("plusHours", plusFn("plusHours"))
                .function("plusMinutes", plusFn("plusMinutes"))
                .function("plusSeconds", plusFn("plusSeconds"))
                .function("plusMilliseconds", plusFn("plusMilliseconds"))
                .function("plusNanos", plusFn("plusNanos"));
    }

    private static QLFunctions.Builder registerAggregates(QLFunctions.Builder builder) {
        return builder

                // "min"/"max"/"avg"/"median" preserve the receiver type, so they are polymorphic. "sum"/"cumSum"
                // are numeric-only, and are registered with a fixed NUMERIC return: a fixed return keeps their
                // call sites single-alternative in the grammar
                .function("min", aggFn("min"))
                .function("min", filteredAggFn("min"))
                .function("max", aggFn("max"))
                .function("max", filteredAggFn("max"))
                .function("avg", aggFn("avg"))
                .function("avg", filteredAggFn("avg"))
                .function("median", aggFn("median"))
                .function("median", filteredAggFn("median"))

                .function("quantile", signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> quantile(args.get(0), constantDouble(args.get(1)), null)))

                .function("quantile", signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .arg(BOOLEAN)
                        .as(args -> quantile(
                                args.get(0),
                                constantDouble(args.get(1)),
                                cond("quantile", args.get(2)))))

                .function("sum", signature()
                        .returning(NUMERIC)
                        .arg(NUMERIC)
                        .as(args -> num("sum", args.get(0)).sum()))

                .function("sum", signature()
                        .returning(NUMERIC)
                        .arg(NUMERIC)
                        .arg(BOOLEAN)
                        .as(args -> num("sum", args.get(0)).sum(cond("sum", args.get(1)))))

                // no filter is supported by "cumSum"
                .function("cumSum", signature()
                        .returning(NUMERIC)
                        .arg(NUMERIC)
                        .as(args -> num("cumSum", args.get(0)).cumSum()))

                // "first"/"last" have no typed expression implementation - they produce a FirstExp/LastExp whose
                // value type is only recoverable at eval time - so they are declared as returning ANY, reachable
                // only from the untyped expression position
                .function("first", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .as(args -> args.get(0).first()))

                .function("first", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .arg(BOOLEAN)
                        .as(args -> args.get(0).first(cond("first", args.get(1)))))

                // TODO: "last" has no filtered overload in the Exp API
                .function("last", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .as(args -> args.get(0).last()))

                // "vConcat" always calls the 4-argument overload, defaulting the filter to null and the prefix and
                // suffix to "". The 1- and 2-argument Exp.vConcat() overloads build a different (ReduceExp2)
                // expression, and the grammar has always used the 4-argument form
                .function("vConcat", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .as(args -> args.get(0).vConcat(null, constantString(args.get(1)), "", "")))

                .function("vConcat", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .arg(BOOLEAN)
                        .constArg(STRING)
                        .as(args -> args.get(0).vConcat(
                                cond("vConcat", args.get(1)),
                                constantString(args.get(2)),
                                "",
                                "")))

                .function("vConcat", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .constArg(STRING)
                        .constArg(STRING)
                        .constArg(STRING)
                        .as(args -> args.get(0).vConcat(
                                null,
                                constantString(args.get(1)),
                                constantString(args.get(2)),
                                constantString(args.get(3)))))

                .function("vConcat", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .arg(BOOLEAN)
                        .constArg(STRING)
                        .constArg(STRING)
                        .constArg(STRING)
                        .as(args -> args.get(0).vConcat(
                                cond("vConcat", args.get(1)),
                                constantString(args.get(2)),
                                constantString(args.get(3)),
                                constantString(args.get(4)))));
    }

    private static QLFunctions.Builder registerSpecialForms(QLFunctions.Builder builder) {
        return builder

                // "if" and "ifNull" produce IfExp/IfNullExp, which implement none of the typed Exp interfaces
                .function("if", signature()
                        .returning(ANY)
                        .arg(BOOLEAN)
                        .arg(OBJECT)
                        .arg(OBJECT)
                        .as(args -> ifExp(cond("if", args.get(0)), args.get(1), args.get(2))))

                .function("ifNull", signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .arg(OBJECT)
                        .as(args -> ifNull(args.get(0), args.get(1))))

                // "shift" is covariantly overridden on every typed expression interface except Condition, so it is
                // polymorphic for an OBJECT receiver and ANY for a boolean one: shifting a Condition produces a
                // plain Exp<Boolean>, not a Condition
                .function("shift", signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> shift(args.get(0), constantInt(args.get(1)), null)))

                .function("shift", signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .constArg(OBJECT)
                        .as(args -> shift(args.get(0), constantInt(args.get(1)), args.get(2))))

                .function("shift", signature()
                        .returning(ANY)
                        .arg(BOOLEAN)
                        .constArg(NUMERIC)
                        .as(args -> shift(args.get(0), constantInt(args.get(1)), null)))

                .function("shift", signature()
                        .returning(ANY)
                        .arg(BOOLEAN)
                        .constArg(NUMERIC)
                        .constArg(OBJECT)
                        .as(args -> shift(args.get(0), constantInt(args.get(1)), args.get(2))));
    }

    // Signature factories for families of functions that differ only by name

    private static QLFunctionSignature fieldFn(String fn) {
        return signature()
                .returning(NUMERIC)
                .arg(OBJECT)
                .as(args -> field(fn, args.get(0)));
    }

    private static QLFunctionSignature plusFn(String fn) {
        return signature()
                .returningArgType(0)
                .arg(OBJECT)
                // the QL integer literal type depends on the literal's magnitude and suffix, so the count is
                // declared numeric and narrowed to an int
                .constArg(NUMERIC)
                .as(args -> plus(fn, args.get(0), constantInt(args.get(1))));
    }

    private static QLFunctionSignature aggFn(String fn) {
        return signature()
                .returningArgType(0)
                .arg(OBJECT)
                .as(args -> agg1(fn, args.get(0), null));
    }

    private static QLFunctionSignature filteredAggFn(String fn) {
        return signature()
                .returningArgType(0)
                .arg(OBJECT)
                .arg(BOOLEAN)
                .as(args -> agg1(fn, args.get(0), cond(fn, args.get(1))));
    }

    // Producers dispatching on the receiver expression interface

    /**
     * Extracts a temporal field from a date, time, datetime or offset datetime expression.
     */
    private static NumExp<Integer> field(String fn, Exp<?> e) {
        return switch (e) {
            case DateExp d -> switch (fn) {
                case "year" -> d.year();
                case "month" -> d.month();
                case "day" -> d.day();
                default -> throw unsupported(fn, e);
            };
            case TimeExp t -> switch (fn) {
                case "hour" -> t.hour();
                case "minute" -> t.minute();
                case "second" -> t.second();
                case "millisecond" -> t.millisecond();
                default -> throw unsupported(fn, e);
            };
            case DateTimeExp dt -> switch (fn) {
                case "year" -> dt.year();
                case "month" -> dt.month();
                case "day" -> dt.day();
                case "hour" -> dt.hour();
                case "minute" -> dt.minute();
                case "second" -> dt.second();
                case "millisecond" -> dt.millisecond();
                default -> throw unsupported(fn, e);
            };
            case OffsetDateTimeExp odt -> switch (fn) {
                case "year" -> odt.year();
                case "month" -> odt.month();
                case "day" -> odt.day();
                case "hour" -> odt.hour();
                case "minute" -> odt.minute();
                case "second" -> odt.second();
                case "millisecond" -> odt.millisecond();
                default -> throw unsupported(fn, e);
            };
            case null, default -> throw unsupported(fn, e);
        };
    }

    /**
     * Adds a number of temporal units to a date, time, datetime or offset datetime expression, preserving the
     * receiver type.
     */
    private static Exp<?> plus(String fn, Exp<?> e, int n) {
        return switch (e) {
            case DateExp d -> switch (fn) {
                case "plusYears" -> d.plusYears(n);
                case "plusMonths" -> d.plusMonths(n);
                case "plusWeeks" -> d.plusWeeks(n);
                case "plusDays" -> d.plusDays(n);
                default -> throw unsupported(fn, e);
            };
            case TimeExp t -> switch (fn) {
                case "plusHours" -> t.plusHours(n);
                case "plusMinutes" -> t.plusMinutes(n);
                case "plusSeconds" -> t.plusSeconds(n);
                case "plusMilliseconds" -> t.plusMilliseconds(n);
                case "plusNanos" -> t.plusNanos(n);
                default -> throw unsupported(fn, e);
            };
            case DateTimeExp dt -> switch (fn) {
                case "plusYears" -> dt.plusYears(n);
                case "plusMonths" -> dt.plusMonths(n);
                case "plusWeeks" -> dt.plusWeeks(n);
                case "plusDays" -> dt.plusDays(n);
                case "plusHours" -> dt.plusHours(n);
                case "plusMinutes" -> dt.plusMinutes(n);
                case "plusSeconds" -> dt.plusSeconds(n);
                case "plusMilliseconds" -> dt.plusMilliseconds(n);
                case "plusNanos" -> dt.plusNanos(n);
                default -> throw unsupported(fn, e);
            };
            case OffsetDateTimeExp odt -> switch (fn) {
                case "plusYears" -> odt.plusYears(n);
                case "plusMonths" -> odt.plusMonths(n);
                case "plusWeeks" -> odt.plusWeeks(n);
                case "plusDays" -> odt.plusDays(n);
                case "plusHours" -> odt.plusHours(n);
                case "plusMinutes" -> odt.plusMinutes(n);
                case "plusSeconds" -> odt.plusSeconds(n);
                case "plusMilliseconds" -> odt.plusMilliseconds(n);
                case "plusNanos" -> odt.plusNanos(n);
                default -> throw unsupported(fn, e);
            };
            case null, default -> throw unsupported(fn, e);
        };
    }

    /**
     * A single-argument aggregate with an optional filter, preserving the receiver type. A null filter produces the
     * same expression as the unfiltered {@code Exp} API overload, which delegates to the filtered one with a null.
     */
    private static Exp<?> agg1(String fn, Exp<?> e, Condition filter) {
        return switch (e) {
            case NumExp<?> n -> switch (fn) {
                case "min" -> n.min(filter);
                case "max" -> n.max(filter);
                case "avg" -> n.avg(filter);
                case "median" -> n.median(filter);
                default -> throw unsupported(fn, e);
            };
            case StrExp s -> switch (fn) {
                // strings have no "avg" or "median"
                case "min" -> s.min(filter);
                case "max" -> s.max(filter);
                default -> throw unsupported(fn, e);
            };
            case DateExp d -> switch (fn) {
                case "min" -> d.min(filter);
                case "max" -> d.max(filter);
                case "avg" -> d.avg(filter);
                case "median" -> d.median(filter);
                default -> throw unsupported(fn, e);
            };
            case TimeExp t -> switch (fn) {
                case "min" -> t.min(filter);
                case "max" -> t.max(filter);
                case "avg" -> t.avg(filter);
                case "median" -> t.median(filter);
                default -> throw unsupported(fn, e);
            };
            case DateTimeExp dt -> switch (fn) {
                case "min" -> dt.min(filter);
                case "max" -> dt.max(filter);
                case "avg" -> dt.avg(filter);
                case "median" -> dt.median(filter);
                default -> throw unsupported(fn, e);
            };
            // OffsetDateTimeExp declares no aggregates at all
            case null, default -> throw unsupported(fn, e);
        };
    }

    private static Exp<?> quantile(Exp<?> e, double q, Condition filter) {
        return switch (e) {
            case NumExp<?> n -> n.quantile(q, filter);
            case DateExp d -> d.quantile(q, filter);
            case TimeExp t -> t.quantile(q, filter);
            case DateTimeExp dt -> dt.quantile(q, filter);
            case null, default -> throw unsupported("quantile", e);
        };
    }

    /**
     * Shifts an expression, optionally filling the gap with a constant. The filler is passed to the erased
     * {@code Exp.shift(int, T)}, which every typed expression interface overrides covariantly, so the receiver's
     * own shift implementation is invoked.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Exp<?> shift(Exp<?> e, int offset, Exp<?> filler) {

        if (filler == null) {
            return e.shift(offset);
        }

        // the filler must be usable as a value of the receiver's own type. The grammar used to enforce this by
        // having a separate shift alternative per receiver type, each with a matching scalar filler rule
        TypeClassifier receiverType = TypeClassifier.classify(e);
        TypeClassifier fillerType = TypeClassifier.classify(filler);
        if (isTyped(receiverType) && isTyped(fillerType) && receiverType != fillerType) {
            throw new IllegalArgumentException("shift() filler of type " + fillerType
                    + " is not compatible with a " + receiverType + " expression: " + e.toQL());
        }

        return ((Exp) e).shift(offset, constantValue(filler));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Exp<?> ifExp(Condition condition, Exp<?> ifTrue, Exp<?> ifFalse) {
        return Exp.ifExp(condition, (Exp) ifTrue, (Exp) ifFalse);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Exp<?> ifNull(Exp<?> exp, Exp<?> ifNull) {
        return Exp.ifNull((Exp) exp, (Exp) ifNull);
    }

    // Argument coercion and error reporting

    private static boolean isTyped(TypeClassifier type) {
        return type != OBJECT && type != ANY;
    }

    private static IllegalArgumentException unsupported(String fn, Exp<?> e) {
        return new IllegalArgumentException(
                fn + "() is not supported for expression: " + (e != null ? e.toQL() : "null"));
    }

    /**
     * Casts an argument to a {@link NumExp}. A parameter declared as NUMERIC also accepts an argument whose type is
     * only known at eval time, which is what this can fail on.
     */
    private static NumExp<?> num(String fn, Exp<?> exp) {
        if (exp instanceof NumExp<?> numExp) {
            return numExp;
        }

        throw unsupported(fn, exp);
    }

    /**
     * Casts an argument to a {@link Condition}. A parameter declared as BOOLEAN also accepts an argument whose type
     * is only known at eval time, which is what this can fail on.
     */
    private static Condition cond(String fn, Exp<?> exp) {
        if (exp instanceof Condition condition) {
            return condition;
        }

        throw new IllegalArgumentException(
                fn + "() expects a boolean expression, got: " + (exp != null ? exp.toQL() : "null"));
    }

    /**
     * Returns the value of a constant expression.
     *
     * @throws IllegalArgumentException if the expression is not a constant
     * @see ScalarExp
     */
    static <T> T constantValue(Exp<T> exp) {
        if (exp instanceof ScalarExp) {
            return exp.reduce((Series<?>) null);
        }

        throw new IllegalArgumentException("Not a constant expression: " + exp.toQL());
    }

    static String constantString(Exp<?> exp) {
        return (String) constantValue(exp);
    }

    /**
     * Reads a constant numeric argument as an int. The QL integer literal type depends on the literal magnitude and
     * suffix (Integer, Long or BigInteger), so the value is narrowed rather than cast. A fractional literal is
     * rejected instead of being silently truncated: these arguments are counts, positions and scales, and the
     * grammar used to accept only integer literals in these positions.
     */
    static int constantInt(Exp<?> exp) {
        Number n = (Number) constantValue(exp);
        if (!(n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte
                || n instanceof BigInteger)) {
            throw new IllegalArgumentException("Not an integer constant: " + exp.toQL());
        }

        return n.intValue();
    }

    static double constantDouble(Exp<?> exp) {
        return ((Number) constantValue(exp)).doubleValue();
    }
}
