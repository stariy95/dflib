package org.dflib.ql.antlr4;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.DecimalExp;
import org.dflib.Environment;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.Series;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.exp.bool.BoolScalarExp;
import org.dflib.exp.flow.IfNullExp;
import org.dflib.exp.str.StrScalarExp;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public static NumExp<Integer> intCol(Object columnId) {
        return col(columnId, Exp::$int, Exp::$int);
    }

    public static NumExp<Long> longCol(Object columnId) {
        return col(columnId, Exp::$long, Exp::$long);
    }

    public static NumExp<Float> floatCol(Object columnId) {
        return col(columnId, Exp::$float, Exp::$float);
    }

    public static NumExp<Double> doubleCol(Object columnId) {
        return col(columnId, Exp::$double, Exp::$double);
    }

    public static NumExp<BigInteger> bigintCol(Object columnId) {
        return col(columnId, Exp::$bigint, Exp::$bigint);
    }

    public static DecimalExp decimalCol(Object columnId) {
        return col(columnId, Exp::$decimal, Exp::$decimal);
    }

    public static StrExp strCol(Object columnId) {
        return col(columnId, Exp::$str, Exp::$str);
    }

    public static Condition boolCol(Object columnId) {
        return col(columnId, Exp::$bool, Exp::$bool);
    }

    public static DateExp dateCol(Object columnId) {
        return col(columnId, Exp::$date, Exp::$date);
    }

    public static TimeExp timeCol(Object columnId) {
        return col(columnId, Exp::$time, Exp::$time);
    }

    public static DateTimeExp dateTimeCol(Object columnId) {
        return col(columnId, Exp::$dateTime, Exp::$dateTime);
    }

    public static OffsetDateTimeExp offsetCol(Object columnId) {
        return col(columnId, Exp::$offsetDateTime, Exp::$offsetDateTime);
    }

    public static Exp<?> col(Object columnId) {
        return col(columnId, Exp::$col, Exp::$col);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Exp<?> ifNullExp(Exp a, Exp b) {
        return new IfNullExp<>(a, b);
    }

    public static <N extends Number> NumExp<N> negate(NumExp<N> num) {
        return num.negate();
    }

    public static NumExp<?> addOrSub(NumExp a, NumExp b, Token op) {
        return switch (op.getType()) {
            case ExpParser.ADD -> a.add(b);
            case ExpParser.SUB -> a.sub(b);
            default -> throw new RuntimeException("Unknown operator: " + op.getText());
        };
    }

    public static NumExp<?> mulDivOrMod(NumExp a, NumExp b, Token op) {
        return switch (op.getType()) {
            case ExpParser.MUL -> a.mul(b);
            case ExpParser.DIV -> a.div(b);
            case ExpParser.MOD -> a.mod(b);
            default -> throw new RuntimeException("Unknown operator: " + op.getText());
        };
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

    public static LocalDate parseDateValue(String token) {
        return LocalDate.parse(token, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalTime parseTimeValue(String token) {
        return LocalTime.parse(token, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public static LocalDateTime parseDateTimeValue(String token) {
        return LocalDateTime.parse(token, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static OffsetDateTime parseOffsetDateTimeValue(String token) {
        return OffsetDateTime.parse(token, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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

    private static <T> T col(Object columnId, Function<Integer, T> byIndex, Function<String, T> byName) {
        if (columnId instanceof Integer) {
            return byIndex.apply((Integer) columnId);
        } else if (columnId instanceof String) {
            return byName.apply((String) columnId);
        } else {
            throw new IllegalArgumentException("An integer or a string expected");
        }
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
        return sanitizeNumScalar(token, radix, null);
    }

    private static String sanitizeNumScalar(String token, int radix, String postfix) {
        String scalar = token.toLowerCase();
        scalar = scalar.replaceAll("_+", "");
        scalar = postfix != null ? scalar.replaceAll(postfix.toLowerCase() + "$", "") : scalar;
        return switch (radix) {
            case 2 -> scalar.replaceFirst("0b", "");
            case 8 -> scalar.replaceFirst("0(?=.)", "");
            case 16 -> scalar.replaceFirst("0x", "");
            default -> scalar;
        };
    }

    public static String unescapeIdentifier(String raw) {
        if (raw == null) {
            return null;
        }

        return raw.replaceAll("``", "`");
    }

    public static String unescapeString(String raw) {
        if (raw == null) {
            return null;
        }

        return raw.replaceAll("''", "'");
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

    @SuppressWarnings("unchecked")
    static <T extends Exp<?>> T param(PositionalParamSource source, BiConsumer<Object, Exp<?>> validator) {
        Object next = source.next();
        Exp<?> exp = val(next);
        validator.accept(next, exp);
        return (T) exp;
    }

    public static Exp<?> param(PositionalParamSource source) {
        return param(source, (o, e) -> {});
    }

    public static NumExp<?> numParam(PositionalParamSource source) {
        return param(source, (o, e) -> {
            if(!(e instanceof NumExp)) {
                throw new RuntimeException("Invalid value for a numeric parameter: " + o);
            }
        });
    }

    public static StrExp strParam(PositionalParamSource source) {
        return param(source, (o, e) -> {
            if(!(e instanceof StrScalarExp)) {
                throw new RuntimeException("Invalid value for a string parameter: " + o);
            }
        });
    }

    public static Condition boolParam(PositionalParamSource source) {
        return param(source, (o, e) -> {
            if(!(e instanceof BoolScalarExp)) {
                throw new RuntimeException("Invalid value for a boolean parameter: " + o);
            }
        });
    }

    public static DateExp dateParam(PositionalParamSource source) {
        return param(source, (o, e) -> {
            if(!(e instanceof DateExp)) {
                throw new RuntimeException("Invalid value for a date parameter: " + o);
            }
        });
    }

    public static TimeExp timeParam(PositionalParamSource source) {
        return param(source, (o, e) -> {
            if(!(e instanceof TimeExp)) {
                throw new RuntimeException("Invalid value for a time parameter: " + o);
            }
        });
    }

    public static DateTimeExp dateTimeParam(PositionalParamSource source) {
        return param(source, (o, e) -> {
            if(!(e instanceof DateTimeExp)) {
                throw new RuntimeException("Invalid value for a datetime parameter: " + o);
            }
        });
    }

    public static OffsetDateTimeExp offsetDateTimeParam(PositionalParamSource source) {
        return param(source, (o, e) -> {
            if(!(e instanceof OffsetDateTimeExp)) {
                throw new RuntimeException("Invalid value for a offset datetime parameter: " + o);
            }
        });
    }

    public static Object[] objArrayParam(PositionalParamSource source) {
        Object next = source.next();
        if(next instanceof Collection) {
            return ((Collection<?>) next).toArray();
        } else if(next instanceof Object[]) {
            return (Object[]) next;
        }
        throw new RuntimeException("Expected array or collection parameter");
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayParam(PositionalParamSource source, IntFunction<T[]> arrayGenerator) {
        Object next = source.next();
        if(next instanceof Collection) {
            return ((Collection<?>) next).toArray(arrayGenerator);
        } else if(next instanceof Object[] data) {
            T[] result = arrayGenerator.apply(data.length);
            for(int i=0; i<data.length; i++) {
                result[i] = (T)data[i]; // unsafe cast here
            }
        }
        throw new RuntimeException("Expected array or collection parameter");
    }

    public static Number[] numArrayParam(PositionalParamSource source) {
        return arrayParam(source, Number[]::new);
    }

    public static String[] strArrayParam(PositionalParamSource source) {
        return arrayParam(source, String[]::new);
    }

    public static Object columnIdParam(PositionalParamSource source) {
        return source.next();
    }

    // Function call dispatch. Every call goes through the untyped "fnCall" rule; the typed rules reach it through
    // name-only predicates (ANTLR only hoists a predicate into prediction if it is reachable without consuming a
    // token) and cast the result with "asX". A polymorphic name is assigned to a rule by the syntax around the call,
    // see "continuation".

    private static QLFunctions functions() {
        return Environment.commonEnv().getQLFunctions();
    }

    public static boolean isFn(String fnName) {
        return functions().isFn(fnName);
    }

    public static boolean mayReturn(String fnName, TypeClassifier type) {
        return functions().mayReturn(fnName, type);
    }

    public static boolean claimedByTyped(String fnName) {
        return functions().hasTypedReturn(fnName);
    }

    public static boolean isPolymorphicFn(String fnName) {
        return functions().isPolymorphicFn(fnName);
    }

    /**
     * The call is followed by an arithmetic or logical operator, or preceded by "not" or a unary minus.
     */
    public static final int CONTINUATION_TYPED = 1;

    /**
     * The call is directly followed by a comparison, "between" or "in".
     */
    public static final int CONTINUATION_COMPARISON = 2;

    public static final int CONTINUATION_NONE = 0;

    /**
     * Classifies the syntax around the call whose name is {@code LT(1)} of the stream as one of the
     * {@code CONTINUATION_*} constants.
     */
    public static int continuation(TokenStream input) {

        Token before = tokenBeforeCall(input);
        if (before != null && (before.getType() == ExpParser.NOT || before.getType() == ExpParser.SUB)) {
            return CONTINUATION_TYPED;
        }

        int close = callCloseOffset(input);

        // "(min(x)) > 5" is a comparison of the parenthesized expression, not of the call
        Token direct = input.LT(close + 1);
        if (direct != null && isComparison(direct.getType())) {
            return CONTINUATION_COMPARISON;
        }

        Token after = input.LT(close + skippedGroupClosings(input, close) + 1);
        return after != null && isTypedOperator(after.getType()) ? CONTINUATION_TYPED : CONTINUATION_NONE;
    }

    private static boolean isTypedOperator(int tokenType) {
        return switch (tokenType) {
            case ExpParser.ADD, ExpParser.SUB, ExpParser.MUL, ExpParser.DIV, ExpParser.MOD,
                 ExpParser.AND, ExpParser.OR -> true;
            default -> false;
        };
    }

    private static boolean isComparison(int tokenType) {
        return switch (tokenType) {
            // "not" past a complete expression can only be the start of "not between" or "not in"
            case ExpParser.GT, ExpParser.GE, ExpParser.LT, ExpParser.LE, ExpParser.EQ, ExpParser.NE,
                 ExpParser.BETWEEN, ExpParser.IN, ExpParser.NOT -> true;
            default -> false;
        };
    }

    /**
     * Returns the first token past the call whose name is {@code LT(1)}, and past the grouping parentheses around it.
     */
    static Token tokenAfterCall(TokenStream input) {
        int close = callCloseOffset(input);
        return input.LT(close + skippedGroupClosings(input, close) + 1);
    }

    /**
     * Returns the {@code LT} offset of the parenthesis closing the call whose name is {@code LT(1)}, or of EOF.
     */
    private static int callCloseOffset(TokenStream input) {

        int k = 2;
        int depth = 0;

        while (true) {
            Token t = input.LT(k);
            if (t == null || t.getType() == Token.EOF) {
                return k;
            }

            if (t.getType() == ExpParser.LP) {
                depth++;
            } else if (t.getType() == ExpParser.RP && --depth <= 0) {
                // "<=": the name may not be a call at all, and the scan must stop at the first unbalanced ")"
                return k;
            }

            k++;
        }
    }

    private static int skippedGroupClosings(TokenStream input, int close) {

        int skipped = 0;
        for (int i = groupingParens(input); i > 0; i--) {
            Token t = input.LT(close + skipped + 1);
            if (t == null || t.getType() != ExpParser.RP) {
                break;
            }
            skipped++;
        }

        return skipped;
    }

    /**
     * Returns the token before the call whose name is {@code LT(1)}, skipping the grouping parentheses around it.
     */
    private static Token tokenBeforeCall(TokenStream input) {
        int i = input.LT(1).getTokenIndex() - groupingParens(input) - 1;
        return i >= 0 ? input.get(i) : null;
    }

    /**
     * Returns the number of parentheses before the call that group it, rather than open an enclosing call.
     */
    private static int groupingParens(TokenStream input) {

        int i = input.LT(1).getTokenIndex() - 1;
        int parens = 0;
        while (i >= 0 && input.get(i).getType() == ExpParser.LP) {
            parens++;
            i--;
        }

        return parens > 0 && i >= 0 && isCallName(input.get(i)) ? parens - 1 : parens;
    }

    /**
     * Returns true if the token can be the name of a call, i.e. is not an operator or literal keyword.
     */
    private static boolean isCallName(Token token) {
        return switch (token.getType()) {
            case ExpParser.NOT, ExpParser.AND, ExpParser.OR, ExpParser.BETWEEN, ExpParser.IN,
                 ExpParser.TRUE, ExpParser.FALSE, ExpParser.NULL,
                 ExpParser.AS, ExpParser.ASC, ExpParser.DESC -> false;
            default -> {
                String text = token.getText();
                char c = text.isEmpty() ? ' ' : text.charAt(0);
                yield Character.isLetter(c) || c == '_' || c == '$';
            }
        };
    }

    /**
     * Resolves a function call against the registry and builds its expression. Resolution and argument errors are
     * reported as a positioned {@link QLParserException}.
     */
    public static Exp<?> fn(Token name, List<Exp<?>> args) {
        List<Arg> argDescriptors = args.stream()
                .map(Arg::of)
                .collect(Collectors.toList());

        try {
            return functions()
                    .function(name.getText(), argDescriptors)
                    .expProducer()
                    .apply(args);
        } catch (IllegalArgumentException e) {
            throw new QLParserException(position(name) + " " + e.getMessage(), e);
        }
    }

    /**
     * Always throws. Declared as returning an expression so that the grammar action can assign it.
     */
    public static Exp<?> unknownFunction(Token name) {
        throw new QLParserException("Unknown function `" + name.getText() + "` at " + position(name));
    }

    public static NumExp<?> asNum(Exp<?> exp, Token name) {
        if (exp instanceof NumExp) {
            return (NumExp<?>) exp;
        }
        throw wrongReturnType(exp, name, "numeric");
    }

    public static StrExp asStr(Exp<?> exp, Token name) {
        if (exp instanceof StrExp) {
            return (StrExp) exp;
        }
        throw wrongReturnType(exp, name, "string");
    }

    public static Condition asCondition(Exp<?> exp, Token name) {
        if (exp instanceof Condition) {
            return (Condition) exp;
        }
        throw wrongReturnType(exp, name, "boolean");
    }

    public static TimeExp asTime(Exp<?> exp, Token name) {
        if (exp instanceof TimeExp) {
            return (TimeExp) exp;
        }
        throw wrongReturnType(exp, name, "time");
    }

    public static DateExp asDate(Exp<?> exp, Token name) {
        if (exp instanceof DateExp) {
            return (DateExp) exp;
        }
        throw wrongReturnType(exp, name, "date");
    }

    public static DateTimeExp asDateTime(Exp<?> exp, Token name) {
        if (exp instanceof DateTimeExp) {
            return (DateTimeExp) exp;
        }
        throw wrongReturnType(exp, name, "datetime");
    }

    public static OffsetDateTimeExp asOffsetDateTime(Exp<?> exp, Token name) {
        if (exp instanceof OffsetDateTimeExp) {
            return (OffsetDateTimeExp) exp;
        }
        throw wrongReturnType(exp, name, "offset datetime");
    }

    private static QLParserException wrongReturnType(Exp<?> exp, Token name, String required) {
        return new QLParserException(name.getText() + "(...) at " + position(name)
                + " returns a " + expTypeLabel(exp) + " expression; a " + required
                + " expression is required here");
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

    // "fnRelation" dispatch: builds the same expressions as the typed relation rules, from the type of the parsed
    // left-hand side

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Condition rel(Exp<?> a, Token op, Exp<?> b) {
        return switch (a) {
            case NumExp n -> numRel(n, op, numOperand(b));
            case StrExp s -> eqOnly(s, op, b, "string");
            case TimeExp t -> timeRel(t, op, b);
            case DateExp d -> dateRel(d, op, b);
            case DateTimeExp dt -> dateTimeRel(dt, op, b);
            case OffsetDateTimeExp odt -> offsetDateTimeRel(odt, op, b);
            case null, default -> eqOnly(a, op, b, "generic");
        };
    }

    // string literals go to "between(String, String)", as in the typed rules: it builds a different expression
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Condition between(Exp<?> a, Exp<?> b, Exp<?> c, boolean negate) {
        String from = temporalStr(b);
        String to = temporalStr(c);

        return switch (a) {
            case NumExp n -> negate
                    ? n.notBetween(numOperand(b), numOperand(c))
                    : n.between(numOperand(b), numOperand(c));
            case TimeExp t -> from != null
                    ? (negate ? t.notBetween(from, to) : t.between(from, to))
                    : (negate ? t.notBetween(timeOperand(b), timeOperand(c)) : t.between(timeOperand(b), timeOperand(c)));
            case DateExp d -> from != null
                    ? (negate ? d.notBetween(from, to) : d.between(from, to))
                    : (negate ? d.notBetween(dateOperand(b), dateOperand(c)) : d.between(dateOperand(b), dateOperand(c)));
            case DateTimeExp dt -> from != null
                    ? (negate ? dt.notBetween(from, to) : dt.between(from, to))
                    : (negate ? dt.notBetween(dateTimeOperand(b), dateTimeOperand(c)) : dt.between(dateTimeOperand(b), dateTimeOperand(c)));
            case OffsetDateTimeExp odt -> from != null
                    ? (negate ? odt.notBetween(from, to) : odt.between(from, to))
                    : (negate ? odt.notBetween(offsetDateTimeOperand(b), offsetDateTimeOperand(c)) : odt.between(offsetDateTimeOperand(b), offsetDateTimeOperand(c)));
            case null, default -> throw new QLParserException(
                    "BETWEEN is not supported for a " + expTypeLabel(a) + " expression");
        };
    }

    private static String temporalStr(Exp<?> exp) {
        return exp instanceof StrScalarExp scalar ? scalar.reduce((Series<?>) null) : null;
    }

    public static Condition in(Exp<?> a, Object[] values, boolean negate) {
        Object[] converted = switch (a) {
            case TimeExp ignored -> mapValues(values, LocalTime::parse, LocalTime[]::new);
            case DateExp ignored -> mapValues(values, LocalDate::parse, LocalDate[]::new);
            case DateTimeExp ignored -> mapValues(values, LocalDateTime::parse, LocalDateTime[]::new);
            case OffsetDateTimeExp ignored -> mapValues(values, OffsetDateTime::parse, OffsetDateTime[]::new);
            case null, default -> values;
        };

        return negate ? a.notIn(converted) : a.in(converted);
    }

    private static <T> T[] mapValues(Object[] values, Function<String, T> parser, IntFunction<T[]> generator) {
        return Arrays.stream(values).map(v -> parser.apply((String) v)).toArray(generator);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Condition numRel(NumExp a, Token op, NumExp b) {
        return switch (op.getType()) {
            case ExpParser.GT -> a.gt(b);
            case ExpParser.GE -> a.ge(b);
            case ExpParser.LT -> a.lt(b);
            case ExpParser.LE -> a.le(b);
            case ExpParser.EQ -> a.eq(b);
            case ExpParser.NE -> a.ne(b);
            default -> throw new QLParserException("Unknown operator: " + op.getText());
        };
    }

    private static Condition eqOnly(Exp<?> a, Token op, Exp<?> b, String label) {
        return switch (op.getType()) {
            case ExpParser.EQ -> a.eq(b);
            case ExpParser.NE -> a.ne(b);
            default -> throw new QLParserException(
                    "Operator '" + op.getText() + "' is not supported for a " + label + " expression");
        };
    }

    private static Condition timeRel(TimeExp a, Token op, Exp<?> b) {
        TimeExp rhs = timeOperand(b);
        return switch (op.getType()) {
            case ExpParser.GT -> a.gt(rhs);
            case ExpParser.GE -> a.ge(rhs);
            case ExpParser.LT -> a.lt(rhs);
            case ExpParser.LE -> a.le(rhs);
            case ExpParser.EQ -> a.eq(rhs);
            case ExpParser.NE -> a.ne(rhs);
            default -> throw new QLParserException("Unknown operator: " + op.getText());
        };
    }

    private static Condition dateRel(DateExp a, Token op, Exp<?> b) {
        DateExp rhs = dateOperand(b);
        return switch (op.getType()) {
            case ExpParser.GT -> a.gt(rhs);
            case ExpParser.GE -> a.ge(rhs);
            case ExpParser.LT -> a.lt(rhs);
            case ExpParser.LE -> a.le(rhs);
            case ExpParser.EQ -> a.eq(rhs);
            case ExpParser.NE -> a.ne(rhs);
            default -> throw new QLParserException("Unknown operator: " + op.getText());
        };
    }

    private static Condition dateTimeRel(DateTimeExp a, Token op, Exp<?> b) {
        DateTimeExp rhs = dateTimeOperand(b);
        return switch (op.getType()) {
            case ExpParser.GT -> a.gt(rhs);
            case ExpParser.GE -> a.ge(rhs);
            case ExpParser.LT -> a.lt(rhs);
            case ExpParser.LE -> a.le(rhs);
            case ExpParser.EQ -> a.eq(rhs);
            case ExpParser.NE -> a.ne(rhs);
            default -> throw new QLParserException("Unknown operator: " + op.getText());
        };
    }

    private static Condition offsetDateTimeRel(OffsetDateTimeExp a, Token op, Exp<?> b) {
        OffsetDateTimeExp rhs = offsetDateTimeOperand(b);
        return switch (op.getType()) {
            case ExpParser.GT -> a.gt(rhs);
            case ExpParser.GE -> a.ge(rhs);
            case ExpParser.LT -> a.lt(rhs);
            case ExpParser.LE -> a.le(rhs);
            case ExpParser.EQ -> a.eq(rhs);
            case ExpParser.NE -> a.ne(rhs);
            default -> throw new QLParserException("Unknown operator: " + op.getText());
        };
    }

    private static NumExp<?> numOperand(Exp<?> exp) {
        if (exp instanceof NumExp) {
            return (NumExp<?>) exp;
        }
        throw operandMismatch(exp, "numeric");
    }

    // a temporal operand is an expression of the type or an ISO-8601 string literal, as in the typed rules

    private static TimeExp timeOperand(Exp<?> exp) {
        return switch (exp) {
            case TimeExp t -> t;
            case StrScalarExp s -> Exp.$timeVal(LocalTime.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(exp, "time");
        };
    }

    private static DateExp dateOperand(Exp<?> exp) {
        return switch (exp) {
            case DateExp d -> d;
            case StrScalarExp s -> Exp.$dateVal(LocalDate.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(exp, "date");
        };
    }

    private static DateTimeExp dateTimeOperand(Exp<?> exp) {
        return switch (exp) {
            case DateTimeExp dt -> dt;
            case StrScalarExp s -> Exp.$dateTimeVal(LocalDateTime.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(exp, "datetime");
        };
    }

    private static OffsetDateTimeExp offsetDateTimeOperand(Exp<?> exp) {
        return switch (exp) {
            case OffsetDateTimeExp odt -> odt;
            case StrScalarExp s -> Exp.$offsetDateTimeVal(OffsetDateTime.parse(temporalStr(s)));
            case null, default -> throw operandMismatch(exp, "offset datetime");
        };
    }

    private static QLParserException operandMismatch(Exp<?> exp, String required) {
        return new QLParserException("A " + required + " expression is required here, got a "
                + expTypeLabel(exp) + " one: " + (exp != null ? exp.toQL() : "null"));
    }
}
