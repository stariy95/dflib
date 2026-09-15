package org.dflib.ql.antlr4;

import org.antlr.v4.runtime.Token;
import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Environment;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.Series;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.exp.str.StrScalarExp;
import org.dflib.ql.QLFunctionArg;
import org.dflib.ql.QLFunctions;
import org.dflib.ql.QLParserException;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Actions of the QL grammar. The grammar is untyped, so the operators check the types of the parsed operands here and
 * report mismatches as positioned {@link QLParserException}s.
 */
class ExpParserUtils {

    private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigDecimal FLOAT_MIN = BigDecimal.valueOf(Float.MIN_NORMAL);
    private static final BigDecimal FLOAT_MAX = BigDecimal.valueOf(Float.MAX_VALUE);
    private static final BigDecimal DOUBLE_MIN = BigDecimal.valueOf(Double.MIN_NORMAL);
    private static final BigDecimal DOUBLE_MAX = BigDecimal.valueOf(Double.MAX_VALUE);

    private static final int FLOAT_SIGNIFICAND = 24;
    private static final int FLOAT_MIN_EXPONENT = -37;
    private static final int FLOAT_MAX_EXPONENT = 37;
    private static final int DOUBLE_SIGNIFICAND = 53;
    private static final int DOUBLE_MIN_EXPONENT = -307;
    private static final int DOUBLE_MAX_EXPONENT = 307;

    // Scalars

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> Exp<T> val(T value) {
        Class type = value != null ? value.getClass() : Object.class;
        return val(value, type);
    }

    @SuppressWarnings("unchecked")
    public static <T, V extends T> Exp<T> val(V value, Class<T> type) {
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return (Exp<T>) Exp.$intVal((Integer) value);
        } else if (Long.class.equals(type) || Long.TYPE.equals(type)) {
            return (Exp<T>) Exp.$longVal((Long) value);
        } else if (Float.class.equals(type) || Float.TYPE.equals(type)) {
            return (Exp<T>) Exp.$floatVal((Float) value);
        } else if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return (Exp<T>) Exp.$doubleVal((Double) value);
        } else if (BigInteger.class.equals(type)) {
            return (Exp<T>) Exp.$bigintVal((BigInteger) value);
        } else if (BigDecimal.class.equals(type)) {
            return (Exp<T>) Exp.$decimalVal((BigDecimal) value);
        } else if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return (Exp<T>) Exp.$boolVal((Boolean) value);
        } else if (String.class.equals(type)) {
            return (Exp<T>) Exp.$strVal((String) value);
        } else if (LocalTime.class.equals(type)) {
            return (Exp<T>) Exp.$timeVal((LocalTime) value);
        } else if (LocalDate.class.equals(type)) {
            return (Exp<T>) Exp.$dateVal((LocalDate) value);
        } else if (LocalDateTime.class.equals(type)) {
            return (Exp<T>) Exp.$dateTimeVal((LocalDateTime) value);
        } else if (OffsetDateTime.class.equals(type)) {
            return (Exp<T>) Exp.$offsetDateTimeVal((OffsetDateTime) value);
        } else {
            return Exp.$val(value);
        }
    }

    public static Object[] objArrayParam(PositionalParamSource source) {
        Object next = source.next();
        if (next instanceof Collection) {
            return ((Collection<?>) next).toArray();
        } else if (next instanceof Object[]) {
            return (Object[]) next;
        }
        throw new RuntimeException("Expected array or collection parameter");
    }

    public static Number parseIntegerValue(String token) {
        int radix = radix(token);
        String sanitizedToken = sanitizeNumScalar(token, radix);
        Matcher matcher = Pattern.compile("(?<number>.+?)[ilh]?").matcher(sanitizedToken);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid integer literal: " + token);
        }

        String number = matcher.group("number");
        if (sanitizedToken.endsWith("i")) {
            return Integer.valueOf(number, radix);
        }
        if (sanitizedToken.endsWith("l")) {
            return Long.valueOf(number, radix);
        }

        BigInteger value = new BigInteger(number, radix);
        if (sanitizedToken.endsWith("h")) {
            return value;
        }

        if (value.compareTo(INT_MIN) >= 0 && value.compareTo(INT_MAX) <= 0) {
            return value.intValue();
        }
        if (value.compareTo(LONG_MIN) >= 0 && value.compareTo(LONG_MAX) <= 0) {
            return value.longValue();
        }
        return value;
    }

    public static Number parseFloatingPointValue(String token) {
        String normalizedToken = token.replaceAll("_+", "").toLowerCase();
        Matcher matcher = Pattern.compile("(?<number>.+?)[fdm]?").matcher(normalizedToken);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid floating point literal: " + token);
        }

        String number = matcher.group("number");
        if (normalizedToken.endsWith("f")) {
            return Float.valueOf(number);
        }
        if (normalizedToken.endsWith("d")) {
            return Double.valueOf(number);
        }

        BigDecimal value = parseDecimalValue(number);
        value = value.stripTrailingZeros();
        if (normalizedToken.endsWith("m")) {
            return value;
        }

        value = value.round(new MathContext(FLOAT_SIGNIFICAND));

        if (mayFitFloat(value)) {
            return value.floatValue();
        }
        if (mayFitDouble(value)) {
            return value.doubleValue();
        }
        return value;
    }

    public static BigDecimal parseDecimalValue(String token) {
        if (token == null) {
            throw new NullPointerException("Input string cannot be null.");
        }
        String normalizedToken = token.replaceAll("_+", "").toLowerCase();
        boolean isNegative = normalizedToken.startsWith("-");
        boolean hasSign = isNegative || normalizedToken.startsWith("+");
        int startIndex = hasSign ? 1 : 0;

        if (!normalizedToken.startsWith("0x", startIndex)) {
            return new BigDecimal(normalizedToken);
        }

        // Extract the main parts of the hex string
        int pIndex = token.indexOf('p', startIndex);
        if (pIndex == -1) {
            throw new NumberFormatException("Input must contain 'p' or 'P' for exponent.");
        }
        String mantissaPart = token.substring(startIndex + 2, pIndex);
        String exponentPart = token.substring(pIndex + 1);

        // Validate and parse the exponent
        int exponent;
        try {
            exponent = Integer.parseInt(exponentPart);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid exponent: " + exponentPart);
        }

        // Handling mantissa
        int hexPointIndex = mantissaPart.indexOf('.');
        BigInteger integerMantissa;
        int fractionalBits = 0;
        if (hexPointIndex >= 0) {
            String integerPart = mantissaPart.substring(0, hexPointIndex);
            String fractionalPart = mantissaPart.substring(hexPointIndex + 1);
            fractionalBits = fractionalPart.length() * 4;
            integerMantissa = new BigInteger(integerPart + fractionalPart, 16);
        } else {
            integerMantissa = new BigInteger(mantissaPart, 16);
        }

        int binaryExponent = exponent - fractionalBits;
        BigDecimal result = new BigDecimal(integerMantissa);
        if (binaryExponent != 0) {
            BigDecimal factor = BigDecimal.valueOf(2).pow(Math.abs(binaryExponent));
            result = binaryExponent > 0 ? result.multiply(factor) : result.divide(factor, MathContext.DECIMAL128);
        }

        return isNegative ? result.negate() : result;
    }

    private static boolean mayFitFloat(BigDecimal value) {
        int significand = value.precision();
        int exponent = value.precision() - value.scale() - 1;
        if (significand > FLOAT_SIGNIFICAND || exponent < FLOAT_MIN_EXPONENT || exponent > FLOAT_MAX_EXPONENT) {
            return false;
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        BigDecimal abs = value.abs();
        return abs.compareTo(FLOAT_MIN) >= 0 && abs.compareTo(FLOAT_MAX) <= 0;
    }

    private static boolean mayFitDouble(BigDecimal value) {
        int significand = value.precision();
        int exponent = value.precision() - value.scale() - 1;
        if (significand > DOUBLE_SIGNIFICAND || exponent < DOUBLE_MIN_EXPONENT || exponent > DOUBLE_MAX_EXPONENT) {
            return false;
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        BigDecimal abs = value.abs();
        return abs.compareTo(DOUBLE_MIN) >= 0 && abs.compareTo(DOUBLE_MAX) <= 0;
    }

    private static int radix(String token) {
        int offset = token.startsWith("+") || token.startsWith("-") ? 1 : 0;
        String lowerToken = token.toLowerCase();
        if (lowerToken.startsWith("0x", offset)) {
            return 16;
        }
        if (lowerToken.startsWith("0b", offset)) {
            return 2;
        }
        return 10;
    }

    private static String sanitizeNumScalar(String token, int radix) {
        String scalar = token.toLowerCase().replaceAll("_+", "");
        return switch (radix) {
            case 2 -> scalar.replaceFirst("0b", "");
            case 8 -> scalar.replaceFirst("0(?=.)", "");
            case 16 -> scalar.replaceFirst("0x", "");
            default -> scalar;
        };
    }

    public static String unescapeIdentifier(String raw) {
        return raw != null ? raw.replaceAll("``", "`") : null;
    }

    public static String unescapeString(String raw) {
        return raw != null ? raw.replaceAll("''", "'") : null;
    }

    // Columns

    /**
     * Builds a column reference of the type named by the keyword token.
     */
    public static Exp<?> col(Token type, Object columnId) {
        return switch (type.getType()) {
            case ExpParser.INT -> col(columnId, Exp::$int, Exp::$int);
            case ExpParser.LONG -> col(columnId, Exp::$long, Exp::$long);
            case ExpParser.BIGINT -> col(columnId, Exp::$bigint, Exp::$bigint);
            case ExpParser.FLOAT -> col(columnId, Exp::$float, Exp::$float);
            case ExpParser.DOUBLE -> col(columnId, Exp::$double, Exp::$double);
            case ExpParser.DECIMAL -> col(columnId, Exp::$decimal, Exp::$decimal);
            case ExpParser.BOOL -> col(columnId, Exp::$bool, Exp::$bool);
            case ExpParser.STR -> col(columnId, Exp::$str, Exp::$str);
            case ExpParser.DATE -> col(columnId, Exp::$date, Exp::$date);
            case ExpParser.TIME -> col(columnId, Exp::$time, Exp::$time);
            case ExpParser.DATETIME -> col(columnId, Exp::$dateTime, Exp::$dateTime);
            case ExpParser.OFFSET_DATETIME -> col(columnId, Exp::$offsetDateTime, Exp::$offsetDateTime);
            case ExpParser.COL -> col(columnId);
            default -> throw new QLParserException("Not a column type: " + type.getText());
        };
    }

    public static Exp<?> col(Object columnId) {
        return col(columnId, Exp::$col, Exp::$col);
    }

    private static <T> T col(Object columnId, Function<Integer, T> byIndex, Function<String, T> byName) {
        if (columnId instanceof Integer) {
            return byIndex.apply((Integer) columnId);
        } else if (columnId instanceof String) {
            return byName.apply((String) columnId);
        } else {
            throw new IllegalArgumentException("An integer or a string expected");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Exp<?> array(Exp exp, String type) {
        Object[] array;
        try {
            array = (Object[]) Array.newInstance(Class.forName(type), 0);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return exp.array(array);
    }

    // Functions

    /**
     * Resolves a function call against the registry and builds its expression. Resolution and argument errors are
     * reported as a positioned {@link QLParserException}.
     */
    public static Exp<?> fn(Token name, List<Exp<?>> args) {

        QLFunctions functions = Environment.commonEnv().getQLFunctions();
        if (!functions.isFn(name.getText())) {
            throw new QLParserException("Unknown function `" + name.getText() + "` at " + position(name));
        }

        List<QLFunctionArg> argDescriptors = args.stream()
                .map(QLFunctionArg::of)
                .collect(Collectors.toList());

        try {
            return functions
                    .function(name.getText(), argDescriptors)
                    .expProducer()
                    .apply(args);
        } catch (IllegalArgumentException e) {
            throw new QLParserException(position(name) + " " + e.getMessage(), e);
        }
    }

    // Operators. Each one checks the operands against the type of expression it applies to.

    public static NumExp<?> negate(Exp<?> a, Token op) {
        return numOperand(a, op).negate();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static NumExp<?> arithmetic(Exp<?> a, Token op, Exp<?> b) {
        NumExp na = numOperand(a, op);
        NumExp nb = numOperand(b, op);
        return switch (op.getType()) {
            case ExpParser.ADD -> na.add(nb);
            case ExpParser.SUB -> na.sub(nb);
            case ExpParser.MUL -> na.mul(nb);
            case ExpParser.DIV -> na.div(nb);
            case ExpParser.MOD -> na.mod(nb);
            default -> throw unknownOperator(op);
        };
    }

    public static Condition not(Exp<?> a, Token op) {
        return Exp.not(boolOperand(a, op));
    }

    public static Condition logical(Exp<?> a, Token op, Exp<?> b) {
        Condition ca = boolOperand(a, op);
        Condition cb = boolOperand(b, op);
        return switch (op.getType()) {
            case ExpParser.AND -> Exp.and(ca, cb);
            case ExpParser.OR -> Exp.or(ca, cb);
            default -> throw unknownOperator(op);
        };
    }

    /**
     * Builds a comparison, dispatching on the type of the left-hand side: numeric and temporal expressions support all
     * the operators, everything else only equality. A temporal expression can be compared with an ISO-8601 string
     * literal, and an untyped expression with anything.
     */
    public static Condition rel(Exp<?> a, Token op, Exp<?> b) {
        return switch (a) {
            case NumExp<?> n -> compare(n, op, numOperand(b, op), (x, y) -> x.gt(y), (x, y) -> x.ge(y),
                    (x, y) -> x.lt(y), (x, y) -> x.le(y));
            case TimeExp t -> compare(t, op, timeOperand(b, op), TimeExp::gt, TimeExp::ge, TimeExp::lt, TimeExp::le);
            case DateExp d -> compare(d, op, dateOperand(b, op), DateExp::gt, DateExp::ge, DateExp::lt, DateExp::le);
            case DateTimeExp dt -> compare(dt, op, dateTimeOperand(b, op),
                    DateTimeExp::gt, DateTimeExp::ge, DateTimeExp::lt, DateTimeExp::le);
            case OffsetDateTimeExp odt -> compare(odt, op, offsetDateTimeOperand(b, op),
                    OffsetDateTimeExp::gt, OffsetDateTimeExp::ge, OffsetDateTimeExp::lt, OffsetDateTimeExp::le);
            case StrExp s -> eqOnly(s, op, b, x -> strOperand(x, op));
            case Condition cond -> eqOnly(cond, op, b, x -> boolOperand(x, op));
            case null, default -> eqOnly(a, op, b, x -> x);
        };
    }

    /**
     * Builds a range check. The bounds of a temporal expression may be ISO-8601 string literals: a pair of them goes to
     * "between(String, String)", which builds a different expression than a pair of temporal scalars, while a single
     * one is converted to a scalar by the operand check.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Condition between(Exp<?> a, Token op, Exp<?> b, Exp<?> c, boolean negate) {
        String from = temporalStr(b);
        String to = temporalStr(c);
        boolean strBounds = from != null && to != null;

        return switch (a) {
            case NumExp n -> negate
                    ? n.notBetween(numOperand(b, op), numOperand(c, op))
                    : n.between(numOperand(b, op), numOperand(c, op));
            case TimeExp t -> strBounds
                    ? (negate ? t.notBetween(from, to) : t.between(from, to))
                    : (negate ? t.notBetween(timeOperand(b, op), timeOperand(c, op)) : t.between(timeOperand(b, op), timeOperand(c, op)));
            case DateExp d -> strBounds
                    ? (negate ? d.notBetween(from, to) : d.between(from, to))
                    : (negate ? d.notBetween(dateOperand(b, op), dateOperand(c, op)) : d.between(dateOperand(b, op), dateOperand(c, op)));
            case DateTimeExp dt -> strBounds
                    ? (negate ? dt.notBetween(from, to) : dt.between(from, to))
                    : (negate ? dt.notBetween(dateTimeOperand(b, op), dateTimeOperand(c, op)) : dt.between(dateTimeOperand(b, op), dateTimeOperand(c, op)));
            case OffsetDateTimeExp odt -> strBounds
                    ? (negate ? odt.notBetween(from, to) : odt.between(from, to))
                    : (negate ? odt.notBetween(offsetDateTimeOperand(b, op), offsetDateTimeOperand(c, op)) : odt.between(offsetDateTimeOperand(b, op), offsetDateTimeOperand(c, op)));
            case null, default -> throw unsupportedOperator(op, a);
        };
    }

    /**
     * Builds an "in" condition, checking the list elements against the type of the left-hand side. Temporal
     * expressions take ISO-8601 strings, an untyped expression takes anything, a boolean expression is not supported.
     */
    public static Condition in(Exp<?> a, Token op, Object[] values, boolean negate) {
        Object[] converted = switch (a) {
            case NumExp<?> ignored -> elements(values, Number.class, op, a);
            case StrExp ignored -> elements(values, String.class, op, a);
            case TimeExp ignored -> mapValues(elements(values, String.class, op, a), LocalTime::parse, LocalTime[]::new);
            case DateExp ignored -> mapValues(elements(values, String.class, op, a), LocalDate::parse, LocalDate[]::new);
            case DateTimeExp ignored -> mapValues(elements(values, String.class, op, a), LocalDateTime::parse, LocalDateTime[]::new);
            case OffsetDateTimeExp ignored -> mapValues(elements(values, String.class, op, a), OffsetDateTime::parse, OffsetDateTime[]::new);
            case Condition ignored -> throw unsupportedOperator(op, a);
            case null, default -> values;
        };

        return negate ? a.notIn(converted) : a.in(converted);
    }

    private static <E extends Exp<?>> Condition compare(
            E a,
            Token op,
            E b,
            BiFunction<E, E, Condition> gt,
            BiFunction<E, E, Condition> ge,
            BiFunction<E, E, Condition> lt,
            BiFunction<E, E, Condition> le) {

        return switch (op.getType()) {
            case ExpParser.GT -> gt.apply(a, b);
            case ExpParser.GE -> ge.apply(a, b);
            case ExpParser.LT -> lt.apply(a, b);
            case ExpParser.LE -> le.apply(a, b);
            case ExpParser.EQ -> a.eq(b);
            case ExpParser.NE -> a.ne(b);
            default -> throw unknownOperator(op);
        };
    }

    // the operator is checked before the operand, so that "'a' > 1" is reported as an unsupported operator
    private static Condition eqOnly(Exp<?> a, Token op, Exp<?> b, Function<Exp<?>, Exp<?>> operand) {
        return switch (op.getType()) {
            case ExpParser.EQ -> a.eq(operand.apply(b));
            case ExpParser.NE -> a.ne(operand.apply(b));
            default -> throw unsupportedOperator(op, a);
        };
    }

    private static <T> T[] elements(Object[] values, Class<T> type, Token op, Exp<?> a) {
        for (Object v : values) {
            if (!type.isInstance(v)) {
                throw new QLParserException("Operator '" + op.getText() + "' at " + position(op) + " expects a list of "
                        + (type == Number.class ? "numeric" : "string") + " values for a " + expTypeLabel(a)
                        + " expression, got: " + v);
            }
        }

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, values.length);
        System.arraycopy(values, 0, result, 0, values.length);
        return result;
    }

    private static <T> T[] mapValues(String[] values, Function<String, T> parser, IntFunction<T[]> generator) {
        return Arrays.stream(values).map(parser).toArray(generator);
    }

    private static String temporalStr(Exp<?> exp) {
        return exp instanceof StrScalarExp scalar ? scalar.reduce((Series<?>) null) : null;
    }

    private static NumExp<?> numOperand(Exp<?> exp, Token op) {
        if (exp instanceof NumExp) {
            return (NumExp<?>) exp;
        }
        throw operandMismatch(op, exp, "numeric");
    }

    private static StrExp strOperand(Exp<?> exp, Token op) {
        if (exp instanceof StrExp) {
            return (StrExp) exp;
        }
        throw operandMismatch(op, exp, "string");
    }

    private static Condition boolOperand(Exp<?> exp, Token op) {
        if (exp instanceof Condition) {
            return (Condition) exp;
        }
        throw operandMismatch(op, exp, "boolean");
    }

    // a temporal operand is an expression of the type or an ISO-8601 string literal

    private static TimeExp timeOperand(Exp<?> exp, Token op) {
        return switch (exp) {
            case TimeExp t -> t;
            case StrScalarExp s -> Exp.$timeVal(LocalTime.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(op, exp, "time");
        };
    }

    private static DateExp dateOperand(Exp<?> exp, Token op) {
        return switch (exp) {
            case DateExp d -> d;
            case StrScalarExp s -> Exp.$dateVal(LocalDate.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(op, exp, "date");
        };
    }

    private static DateTimeExp dateTimeOperand(Exp<?> exp, Token op) {
        return switch (exp) {
            case DateTimeExp dt -> dt;
            case StrScalarExp s -> Exp.$dateTimeVal(LocalDateTime.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(op, exp, "datetime");
        };
    }

    private static OffsetDateTimeExp offsetDateTimeOperand(Exp<?> exp, Token op) {
        return switch (exp) {
            case OffsetDateTimeExp odt -> odt;
            case StrScalarExp s -> Exp.$offsetDateTimeVal(OffsetDateTime.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(op, exp, "offset datetime");
        };
    }

    // Errors

    private static QLParserException operandMismatch(Token op, Exp<?> exp, String required) {
        return new QLParserException("Operator '" + op.getText() + "' at " + position(op) + " expects a " + required
                + " operand, got a " + expTypeLabel(exp) + " one: " + (exp != null ? exp.toQL() : "null"));
    }

    private static QLParserException unsupportedOperator(Token op, Exp<?> exp) {
        return new QLParserException("Operator '" + op.getText() + "' at " + position(op) + " is not supported for a "
                + expTypeLabel(exp) + " expression: " + (exp != null ? exp.toQL() : "null"));
    }

    private static QLParserException unknownOperator(Token op) {
        return new QLParserException("Unknown operator: " + op.getText());
    }

    private static String position(Token token) {
        return token.getLine() + ":" + token.getCharPositionInLine();
    }

    private static String expTypeLabel(Exp<?> exp) {
        return switch (exp) {
            case NumExp<?> ignored -> "numeric";
            case StrExp ignored -> "string";
            case Condition ignored -> "boolean";
            case DateExp ignored -> "date";
            case TimeExp ignored -> "time";
            case DateTimeExp ignored -> "datetime";
            case OffsetDateTimeExp ignored -> "offset datetime";
            case null -> "null";
            default -> "generic";
        };
    }
}
