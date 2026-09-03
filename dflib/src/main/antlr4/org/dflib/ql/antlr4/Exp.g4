grammar Exp;

// Functions are not grammar rules: every call goes through "fnCall" and is resolved by the "QLFunctions" registry.
// The typed rules reach "fnCall" via left-edge predicates, as ANTLR only hoists a predicate into prediction if it is
// reachable without consuming a token, and cast the result. A polymorphic name is assigned to a rule by the syntax
// around the call ("ExpParserUtils.continuation"). Only constructs that are not expression-to-expression calls
// (column references, "array", parameters, operators, literals) remain rules.

@header {
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.dflib.*;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import static org.dflib.ql.antlr4.ExpParserUtils.*;
}

// parser-scoped, as the predicates below read the token stream
@parser::members {
// global state of the parser
PositionalParamSource paramSource;

public void setParameters(Object... params) {
    this.paramSource = new PositionalParamSource(params);
}

// call site continuations, cached per call site as predicates are evaluated more than once
private final java.util.Map<Integer, Integer> continuations = new java.util.HashMap<>();

/**
 * True if the call at the current position is resolved by the untyped "fnCall" alternative of "expression".
 */
boolean untypedCall() {
    String name = _input.LT(1).getText();
    return isFn(name)
        && (!claimedByTyped(name)
            || (isPolymorphicFn(name) && continuation() == ExpParserUtils.CONTINUATION_NONE));
}

/**
 * True if the call at the current position may be resolved by the typed rule producing the given type.
 */
boolean typedCall(TypeClassifier type) {
    String name = _input.LT(1).getText();
    if (!mayReturn(name, type)) {
        return false;
    }

    if (!isPolymorphicFn(name)) {
        return true;
    }

    // "_ctx" is the rule owning the decision: in "expression" the competitor is the untyped alternative, anywhere
    // else the surrounding syntax demands the type and the only competitor is "fnRelation"
    return _ctx instanceof ExpressionContext
        ? continuation() == ExpParserUtils.CONTINUATION_TYPED
        : continuation() != ExpParserUtils.CONTINUATION_COMPARISON;
}

private int continuation() {
    return continuations.computeIfAbsent(
        _input.LT(1).getTokenIndex(),
        i -> ExpParserUtils.continuation(_input)
    );
}
}

/// **Parser rules**

/**
 * The root rule of the grammar.
 */
expRoot returns [Exp<?> exp]
    : expSingle EOF { $exp = $expSingle.exp; }
    ;

expSingle returns [Exp<?> exp] locals [String alias]
    : expression
    (
       AS identifier { $alias = $identifier.id; }
    )?
    { $exp = $alias == null ? $expression.exp : $expression.exp.as($alias); }
    ;

expArray returns [Exp[] expressions]
    : args += expSingle (',' args += expSingle )* EOF
    { $expressions = $args.stream().map(e -> e.exp).toArray(Exp[]::new); }
    ;

/**
 * The root rule for the sorting spec
 */
sorterRoot returns [Sorter sorter]
    : sorterSingle EOF { $sorter = $sorterSingle.sorter; }
    ;

sorterSingle returns [Sorter sorter] locals [boolean desc]
    : expression (
        : ASC
        | DESC { $desc = true; }
    )? { $sorter = $desc ? $expression.exp.desc() : $expression.exp.asc(); }
    ;

sorterArray returns [Sorter[] sorters]
    : args += sorterSingle (',' args += sorterSingle)* EOF
    { $sorters = $args.stream().map(s -> s.sorter).toArray(Sorter[]::new); }
    ;

/**
 * An expression, which can be of various types including null, aggregate, boolean, numeric, string, temporal, or type-agnostic functions.
 * An expression represents a single value or a combination of values, operators, and functions.
 */
expression returns [Exp<?> exp]
    // parenthesized expressions come first, or a typed rule's "'(' X ')'" would claim "(min(x))" and fail
    : '(' expression ')' { $exp = $expression.exp; }
    | { untypedCall() }? fnCall { $exp = $fnCall.exp; }
    | PARAMETER { $exp = val(paramSource.next()); }
    | boolExp { $exp = $boolExp.exp; }
    | numExp { $exp = $numExp.exp; }
    | strExp { $exp = $strExp.exp; }
    | temporalExp { $exp = $temporalExp.exp; }
    | genericExp { $exp = $genericExp.exp; }
    | array { $exp = $array.exp; }
    | NULL { $exp = val(null); }
    // a call by an unregistered name, reported as such rather than as a syntax error
    | { !isFn(_input.LT(1).getText()) }? IDENTIFIER '(' (expression (',' expression)*)? ')' {
        $exp = unknownFunction($IDENTIFIER);
    }
    ;

/**
 * A call of a function from the QL function registry, resolved by name and argument types.
 */
fnCall returns [Exp<?> exp]
    : { isFn(_input.LT(1).getText()) }? IDENTIFIER '(' (args+=expression (',' args+=expression)*)? ')' {
        $exp = fn($IDENTIFIER, $args.stream().map(ctx -> ctx.exp).collect(Collectors.toList()));
    }
    ;

/// **Numeric expressions**

/**
 * Numeric expressions, encompassing scalar values, column references, functions,
 * aggregates, and arithmetic operations.
 */
numExp returns [NumExp<?> exp]
    : numScalar { $exp = (NumExp<?>) val($numScalar.value); }
    | PARAMETER { $exp = numParam(paramSource); }
    | numColumn { $exp = $numColumn.exp; }
    | { typedCall(TypeClassifier.NUMERIC) }? fnCall { $exp = asNum($fnCall.exp, $fnCall.start); }
    | SUB numExp { $exp = negate($numExp.exp); }
    | a=numExp op=(MUL | DIV | MOD) b=numExp { $exp = mulDivOrMod($a.exp, $b.exp, $op); }
    | a=numExp op=(ADD | SUB) b=numExp { $exp = addOrSub($a.exp, $b.exp, $op); }
    | '(' numExp ')' { $exp = $numExp.exp; }
    ;

/// **Boolean expressions**

/**
 * Boolean expressions, which evaluate to true or false. These can include
 * boolean scalar values, column references, boolean functions, comparisons (relations),
 * and logical operations (AND, OR, NOT).
 */
boolExp returns [Condition exp]
    : boolScalar { $exp = Exp.\$boolVal($boolScalar.value); }
    | PARAMETER { $exp = boolParam(paramSource); }
    | boolColumn { $exp = $boolColumn.exp; }
    // "relation" must come before the function hook, as only it can consume a comparison of a call
    | relation { $exp = $relation.exp; }
    | { typedCall(TypeClassifier.BOOLEAN) }? fnCall { $exp = asCondition($fnCall.exp, $fnCall.start); }
    | NOT boolExp { $exp = Exp.not($boolExp.exp); }
    | a=boolExp AND b=boolExp { $exp = Exp.and($a.exp, $b.exp); }
    | a=boolExp OR b=boolExp { $exp = Exp.or($a.exp, $b.exp); }
    | a=boolExp EQ b=boolExp { $exp = $a.exp.eq($b.exp); }
    | a=boolExp NE b=boolExp { $exp = $a.exp.ne($b.exp); }
    | '(' boolExp ')' { $exp = $boolExp.exp; }
    ;

/// **String expressions**

/**
 * String expressions, including string literals, column references, and
 * string manipulation functions.
 */
strExp returns [StrExp exp]
    : strScalar { $exp = Exp.\$strVal($strScalar.value); }
    | PARAMETER { $exp = strParam(paramSource); }
    | strColumn { $exp = $strColumn.exp; }
    | { typedCall(TypeClassifier.STRING) }? fnCall { $exp = asStr($fnCall.exp, $fnCall.start); }
    | '(' strExp ')' { $exp = $strExp.exp; }
    ;

/// **Temporal expressions**

/**
 * Temporal expressions, encompassing time, date, and datetime values.
 */
temporalExp returns [Exp<? extends Temporal> exp]
    : timeExp { $exp = $timeExp.exp; }
    | dateExp { $exp = $dateExp.exp; }
    | dateTimeExp { $exp = $dateTimeExp.exp; }
    | offsetDateTimeExp { $exp = $offsetDateTimeExp.exp; }
    | '(' temporalExp ')' { $exp = $temporalExp.exp; }
    ;

/**
 * Expressions representing a time of day. These may include column references
 * and time functions.
 */
timeExp returns [TimeExp exp]
    : timeColumn { $exp = $timeColumn.exp; }
    | { typedCall(TypeClassifier.TIME) }? fnCall { $exp = asTime($fnCall.exp, $fnCall.start); }
    | PARAMETER { $exp = timeParam(paramSource); }
    ;

/**
 * Date expressions, which can include references to date columns and date functions.
 */
dateExp returns [DateExp exp]
    : dateColumn { $exp = $dateColumn.exp; }
    | { typedCall(TypeClassifier.DATE) }? fnCall { $exp = asDate($fnCall.exp, $fnCall.start); }
    | PARAMETER { $exp = dateParam(paramSource); }
    ;

/**
 * Datetime expressions, which can refer to datetime columns and utilize datetime functions.
 */
dateTimeExp returns [DateTimeExp exp]
    : dateTimeColumn { $exp = $dateTimeColumn.exp; }
    | { typedCall(TypeClassifier.DATETIME) }? fnCall { $exp = asDateTime($fnCall.exp, $fnCall.start); }
    | PARAMETER { $exp = dateTimeParam(paramSource); }
    ;

/**
 * Datetime expressions with an offset from UTC+0.
 *
 * This is essential for handling timezones correctly.
 */
offsetDateTimeExp returns [OffsetDateTimeExp exp]
    : offsetDateTimeColumn { $exp = $offsetDateTimeColumn.exp; }
    | { typedCall(TypeClassifier.OFFSETDATETIME) }? fnCall {
        $exp = asOffsetDateTime($fnCall.exp, $fnCall.start);
    }
    | PARAMETER { $exp = offsetDateTimeParam(paramSource); }
    ;

/// **Generic expressions**

/**
 *  Expressions with no type specified.
 */
genericExp returns [Exp<?> exp]
    : genericColumn { $exp = $genericColumn.exp; }
    | '(' genericExp ')' { $exp = $genericExp.exp; }
    ;

/// **Scalar expressions**

anyScalar returns [Object value]
    : boolScalar { $value = $boolScalar.value; }
    | numScalar { $value = $numScalar.value; }
    | strScalar { $value = $strScalar.value; }
    | PARAMETER { $value = paramSource.next(); }
    ;

/**
 * List of a comma-sperated scalars
 */
anyScalarList returns [Object[] value]
    : '(' values+=anyScalar (',' values+=anyScalar)* ')' { $value = $values.stream().map(a -> a.value).toArray(); }
    | PARAMETER { $value = objArrayParam(paramSource); }
    ;

/**
 * Boolean scalar value (true or false).
 */
boolScalar returns [Boolean value]
    : TRUE { $value = true; }
    | FALSE { $value = false; }
    ;

/**
 * Numeric scalar values (literals), which can be integer or floating-point numbers.
 */
numScalar returns [Number value]
    : integerScalar { $value = $integerScalar.value; }
    | floatingPointScalar { $value = $floatingPointScalar.value; }
    ;

/**
 * List of a comma-sperated numeric scalars
 */
numScalarList returns [Number[] value] locals [List<Number> values = new ArrayList<>()]
    :
    '('
        numScalarOrParamter { $values.add($numScalarOrParamter.value); }
        (',' numScalarOrParamter { $values.add($numScalarOrParamter.value); })*
    ')'
        { $value = $values.toArray(Number[]::new); }
    | PARAMETER { $value = numArrayParam(paramSource); }
    ;

numScalarOrParamter returns [Number value]
    : numScalar { $value = $numScalar.value; } | PARAMETER { $value = paramSource.next(Number.class); }
    ;

/**
 * An integer scalar value of any size.
 */
integerScalar returns [Number value]
    : INTEGER_LITERAL { $value = parseIntegerValue($text); }
    ;

/**
 * A floating-point scalar value of any scale and precision.
 */
floatingPointScalar returns [Number value]
    : FLOAT_LITERAL { $value = parseFloatingPointValue($text); }
    ;

/**
 * Time string in ISO-8601 compatible format (`hh:mm[:ss[.sss]]`)
 */
timeStrScalar returns [String value]
    // this is just an alias for a string, validation done in runtime
    : strScalar { $value = $strScalar.value; }
    ;

/**
 * Time string in ISO-8601 compatible format (`YYYY-MM-DD`)
 */
dateStrScalar returns [String value]
    // this is just an alias for a string, validation done in runtime
    : strScalar { $value = $strScalar.value; }
    ;

/**
 * Time string in ISO-8601 compatible format (`<date>T<time>`)
 */
dateTimeStrScalar returns [String value]
    // this is just an alias for a string, validation done in runtime
    : strScalar { $value = $strScalar.value; }
    ;

/**
 * A dateTime with offset string in ISO-8601 compatible format  (`<date>T<time>Z`)
 */
offsetDateTimeStrScalar returns [String value]
    // this is just an alias for a string, validation done in runtime
    : strScalar { $value = $strScalar.value; }
    ;

/**
 * A string literal.
 */
strScalar returns [String value]
    : STRING_LITERAL { $value = unescapeString($text.substring(1, $text.length() - 1)); }
    ;

/**
 * List of a comma-sperated string literals
 */
strScalarList returns [String[] value] locals [List<String> values = new ArrayList<>()]
    :
    '('
        strScalarOrParameter { $values.add($strScalarOrParameter.value); }
        (',' strScalarOrParameter { $values.add($strScalarOrParameter.value); })*
    ')'
        { $value = $values.toArray(String[]::new); }
    | PARAMETER { $value = strArrayParam(paramSource); }
    ;

strScalarOrParameter returns [String value]
    : strScalar { $value = $strScalar.value; } | PARAMETER { $value = paramSource.next(String.class); }
    ;

/// **Column expressions**

/**
 * An expression referencing a numeric column.  Supports various numeric types.
 */
numColumn returns [NumExp<?> exp]
    : intColumn { $exp = $intColumn.exp; }
    | longColumn { $exp = $longColumn.exp; }
    | bigintColumn { $exp = $bigintColumn.exp; }
    | floatColumn { $exp = $floatColumn.exp; }
    | doubleColumn { $exp = $doubleColumn.exp; }
    | decimalColumn { $exp = $decimalColumn.exp; }
    ;

/**
 * An expression referencing a column of integer values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
intColumn returns [NumExp<Integer> exp]
    : INT '(' columnId ')' { $exp = intCol($columnId.id); }
    ;

/**
 * An expression referencing a column containing long integer values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
longColumn returns [NumExp<Long> exp]
    : LONG '(' columnId ')' { $exp = longCol($columnId.id); }
    ;

/**
 * An expression referencing a column containing BigInteger values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
bigintColumn returns [NumExp<BigInteger> exp]
    : BIGINT '(' columnId ')' { $exp = bigintCol($columnId.id); }
    ;

/**
 * An expression that references a column of float values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
floatColumn returns [NumExp<Float> exp]
    : FLOAT '(' columnId ')' { $exp = floatCol($columnId.id); }
    ;

/**
 * An expression referencing a column containing double-precision floating-point numbers.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
doubleColumn returns [NumExp<Double> exp]
    : DOUBLE '(' columnId ')' { $exp = doubleCol($columnId.id); }
    ;

/**
 * An expression referencing a column of Decimal values (for high-precision arithmetic).
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name)n.
 */
decimalColumn returns [DecimalExp exp]
    : DECIMAL '(' columnId ')' { $exp = decimalCol($columnId.id); }
    ;

/**
 * An expression that accesses a column of boolean values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
boolColumn returns [Condition exp]
    : BOOL '(' columnId ')' { $exp = boolCol($columnId.id); }
    ;

/**
 * An expression referring to a column containing string values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
strColumn returns [StrExp exp]
    : STR '(' columnId ')' { $exp = strCol($columnId.id); }
    ;

/**
 * An expression referencing a column containing Date values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
dateColumn returns [DateExp exp]
    : DATE '(' columnId ')' { $exp = dateCol($columnId.id); }
    ;

/**
 * An expression that refers to a column containing Time values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
timeColumn returns [TimeExp exp]
    : TIME '(' columnId ')' { $exp = timeCol($columnId.id); }
    ;

/**
 * An expression referencing a column storing DateTime values.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
dateTimeColumn returns [DateTimeExp exp]
    : DATETIME '(' columnId ')' { $exp = dateTimeCol($columnId.id); }
    ;

/**
 * An expression referencing a column containing OffsetDateTime values (datetime with timezone offset).
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
offsetDateTimeColumn returns [OffsetDateTimeExp exp]
    : OFFSET_DATETIME '(' columnId ')' { $exp = offsetCol($columnId.id); }
    ;

/**
 * An expression referencing a column with a non-specified type.
 *
 * Parameters:
 *  - The identifier of the column (integer index or string name).
 */
genericColumn returns [Exp<?> exp]
    : COL '(' columnId ')' { $exp = col($columnId.id); }
    | identifier { $exp = Exp.\$col($identifier.id); }
    ;

/**
 * A column identifier, which can be an integer representing the column index or a string representing the column name.
 */
columnId returns [Object id]
    : integerScalar { $id = $integerScalar.value; }
    | identifier { $id = $identifier.id; }
    | PARAMETER { $id = columnIdParam(paramSource); }
    ;

/**
 * An identifier, which is a sequence of letters and digits starting with a letter.
 */
//@ doc:inline
identifier returns [String id]
    : IDENTIFIER { $id = $text; }
    | QUOTED_IDENTIFIER { $id = unescapeIdentifier($text.substring(1, $text.length() - 1)); }
    | fnName { $id = $fnName.id; }
    ;

/// **Relational expressions**

/**
 * Relational expressions, which can include numeric, string, time, date, and datetime relations.
 * These expressions compare two values using operators like >, <, =, !=, etc.
 */
relation returns [Condition exp]
    : { isPolymorphicFn(_input.LT(1).getText()) }? fnRelation { $exp = $fnRelation.exp; }
    | numRelation { $exp = $numRelation.exp; }
    | strRelation { $exp = $strRelation.exp; }
    | timeRelation { $exp = $timeRelation.exp; }
    | dateRelation { $exp = $dateRelation.exp; }
    | dateTimeRelation { $exp = $dateTimeRelation.exp; }
    | offsetDateTimeRelation { $exp = $offsetDateTimeRelation.exp; }
    | genericRelation { $exp = $genericRelation.exp; }
    | '(' relation ')' { $exp = $relation.exp; }
    ;

/**
 * A relational expression whose left-hand side is a call of a polymorphic function. The comparison is built by
 * dispatching on the type of the expression the call produced.
 *
 * Parameters:
 *  - The left-hand side function call.
 *  - The right-hand side expression.
 *  - The upper bound expression (for BETWEEN).
 */
fnRelation returns [Condition exp]
    : a=fnCall (
        : op=(GT | GE | LT | LE | EQ | NE) b=expression { $exp = rel($a.exp, $op, $b.exp); }
        | BETWEEN b=expression AND c=expression { $exp = between($a.exp, $b.exp, $c.exp, false); }
        | NOT BETWEEN b=expression AND c=expression { $exp = between($a.exp, $b.exp, $c.exp, true); }
        | IN l=anyScalarList { $exp = in($a.exp, $l.value, false); }
        | NOT IN l=anyScalarList { $exp = in($a.exp, $l.value, true); }
    )
    ;

/**
 * A numeric relational expression. This compares two numeric expressions
 * using comparison operators (>, >=, <, <=, =, !=, BETWEEN).
 *
 * Parameters:
 *  - The left-hand side numeric expression.
 *  - The right-hand side numeric expression.
 *  - The upper bound numeric expression (for BETWEEN).
 */
numRelation returns [Condition exp] locals [BiFunction<NumExp<?>, NumExp<?>, Condition> rel]
    : a=numExp (
        : (
            : GT { $rel = (a, b) -> a.gt(b); }
            | GE { $rel = (a, b) -> a.ge(b); }
            | LT { $rel = (a, b) -> a.lt(b); }
            | LE { $rel = (a, b) -> a.le(b); }
            | EQ { $rel = (a, b) -> a.eq(b); }
            | NE { $rel = (a, b) -> a.ne(b); }
        ) b=numExp { $exp = $rel.apply($a.exp, $b.exp); }
        | BETWEEN b=numExp AND c=numExp { $exp = $a.exp.between($b.exp, $c.exp); }
        | NOT BETWEEN b=numExp AND c=numExp { $exp = $a.exp.notBetween($b.exp, $c.exp); }
        | IN l=numScalarList { $exp = $a.exp.in($l.value); }
        | NOT IN l=numScalarList { $exp = $a.exp.notIn($l.value); }
    )
    ;

/**
 * String relational expressions. Compares two string expressions using either
 * equality (=) or inequality (!=).
 *
 * Parameters:
 *  - The left-hand side string expression.
 *  - The right-hand side string expression.
 */
strRelation returns [Condition exp] locals [BiFunction<StrExp, StrExp, Condition> rel]
    : a=strExp (
        : (
            : EQ { $rel = (a, b) -> a.eq(b); }
            | NE { $rel = (a, b) -> a.ne(b); }
        ) b=strExp { $exp = $rel.apply($a.exp, $b.exp); }
        | IN l=strScalarList { $exp = $a.exp.in($l.value); }
        | NOT IN l=strScalarList { $exp = $a.exp.notIn($l.value); }
    )
    ;

/**
 * Time relational expressions. Compares two time expressions using comparison operators.
 *
 * Parameters:
 *  - The left-hand TimeExp.
 *  - The right-hand TimeExp.
 *  - The upper bound TimeExp (for BETWEEN).
 */
timeRelation returns [Condition exp] locals [BiFunction<TimeExp, TimeExp, Condition> rel]
    : a=timeExp (
        : (
            : GT { $rel = (a, b) -> a.gt(b); }
            | GE { $rel = (a, b) -> a.ge(b); }
            | LT { $rel = (a, b) -> a.lt(b); }
            | LE { $rel = (a, b) -> a.le(b); }
            | EQ { $rel = (a, b) -> a.eq(b); }
            | NE { $rel = (a, b) -> a.ne(b); }
        ) (
            : b=timeExp { $exp = $rel.apply($a.exp, $b.exp); }
            | s=timeStrScalar { $exp = $rel.apply($a.exp, Exp.\$timeVal(LocalTime.parse($s.value))); }
        )
        | BETWEEN (
            b=timeExp AND c=timeExp { $exp = $a.exp.between($b.exp, $c.exp); }
            | s1=timeStrScalar AND s2=timeStrScalar { $exp = $a.exp.between($s1.value, $s2.value); }
        )
        | NOT BETWEEN (
            b=timeExp AND c=timeExp { $exp = $a.exp.notBetween($b.exp, $c.exp); }
            | s1=timeStrScalar AND s2=timeStrScalar { $exp = $a.exp.notBetween($s1.value, $s2.value); }
        )
        | IN l=strScalarList { $exp = $a.exp.in(Arrays.stream($l.value).map(LocalTime::parse).toArray(LocalTime[]::new)); }
        | NOT IN l=strScalarList { $exp = $a.exp.notIn(Arrays.stream($l.value).map(LocalTime::parse).toArray(LocalTime[]::new)); }
    )
    ;

/**
 * Date relational expressions, comparing two DateExps.
 *
 * Parameters:
 *  - The left-hand DateExp.
 *  - The right-hand DateExp.
 *  - The upper bound DateExp (for BETWEEN).
 */
dateRelation returns [Condition exp] locals [BiFunction<DateExp, DateExp, Condition> rel]
    : a=dateExp (
        : (
            : GT { $rel = (a, b) -> a.gt(b); }
            | GE { $rel = (a, b) -> a.ge(b); }
            | LT { $rel = (a, b) -> a.lt(b); }
            | LE { $rel = (a, b) -> a.le(b); }
            | EQ { $rel = (a, b) -> a.eq(b); }
            | NE { $rel = (a, b) -> a.ne(b); }
        ) (
            : b=dateExp { $exp = $rel.apply($a.exp, $b.exp); }
            | s=dateStrScalar { $exp = $rel.apply($a.exp, Exp.\$dateVal(LocalDate.parse($s.value))); }
        )
        | BETWEEN (
            b=dateExp AND c=dateExp { $exp = $a.exp.between($b.exp, $c.exp); }
            | s1=dateStrScalar AND s2=dateStrScalar { $exp = $a.exp.between($s1.value, $s2.value); }
        )
        | NOT BETWEEN (
            b=dateExp AND c=dateExp { $exp = $a.exp.notBetween($b.exp, $c.exp); }
            | s1=dateStrScalar AND s2=dateStrScalar { $exp = $a.exp.notBetween($s1.value, $s2.value); }
        )
        | IN l=strScalarList { $exp = $a.exp.in(Arrays.stream($l.value).map(LocalDate::parse).toArray(LocalDate[]::new)); }
        | NOT IN l=strScalarList { $exp = $a.exp.notIn(Arrays.stream($l.value).map(LocalDate::parse).toArray(LocalDate[]::new)); }
    )
    ;

/**
 * A datetime relational expression. Compares two DateTimeExp values.
 *
 * Parameters:
 *  - The left-hand DateTimeExp.
 *  - The right-hand DateTimeExp.
 *  - The upper bound DateTimeExp (for BETWEEN).
 */
dateTimeRelation returns [Condition exp] locals [BiFunction<DateTimeExp, DateTimeExp, Condition> rel]
    : a=dateTimeExp (
        : (
            : GT { $rel = (a, b) -> a.gt(b); }
            | GE { $rel = (a, b) -> a.ge(b); }
            | LT { $rel = (a, b) -> a.lt(b); }
            | LE { $rel = (a, b) -> a.le(b); }
            | EQ { $rel = (a, b) -> a.eq(b); }
            | NE { $rel = (a, b) -> a.ne(b); }
        ) (
            : b=dateTimeExp { $exp = $rel.apply($a.exp, $b.exp); }
            | s=dateTimeStrScalar { $exp = $rel.apply($a.exp, Exp.\$dateTimeVal(LocalDateTime.parse($s.value))); }
        )
        | BETWEEN (
            b=dateTimeExp AND c=dateTimeExp { $exp = $a.exp.between($b.exp, $c.exp); }
            | s1=dateTimeStrScalar AND s2=dateTimeStrScalar { $exp = $a.exp.between($s1.value, $s2.value); }
        )
        | NOT BETWEEN (
            b=dateTimeExp AND c=dateTimeExp { $exp = $a.exp.notBetween($b.exp, $c.exp); }
            | s1=dateTimeStrScalar AND s2=dateTimeStrScalar { $exp = $a.exp.notBetween($s1.value, $s2.value); }
        )
        | IN l=strScalarList { $exp = $a.exp.in(Arrays.stream($l.value).map(LocalDateTime::parse).toArray(LocalDateTime[]::new)); }
        | NOT IN l=strScalarList { $exp = $a.exp.notIn(Arrays.stream($l.value).map(LocalDateTime::parse).toArray(LocalDateTime[]::new)); }
    )
    ;

/**
 * An OffsetDateTime relational expression. Compares two OffsetDateTimeExp values.
 *
 * Parameters:
 *  - The left-hand OffsetDateTimeExp.
 *  - The right-hand OffsetDateTimeExp.
 *  - The upper bound OffsetDateTimeExp (for BETWEEN).
 */
offsetDateTimeRelation returns [Condition exp] locals [BiFunction<OffsetDateTimeExp, OffsetDateTimeExp, Condition> rel]
    : a=offsetDateTimeExp (
        : (
            : GT { $rel = (a, b) -> a.gt(b); }
            | GE { $rel = (a, b) -> a.ge(b); }
            | LT { $rel = (a, b) -> a.lt(b); }
            | LE { $rel = (a, b) -> a.le(b); }
            | EQ { $rel = (a, b) -> a.eq(b); }
            | NE { $rel = (a, b) -> a.ne(b); }
        ) (
            : b=offsetDateTimeExp { $exp = $rel.apply($a.exp, $b.exp); }
            | s=offsetDateTimeStrScalar { $exp = $rel.apply($a.exp, Exp.\$offsetDateTimeVal(OffsetDateTime.parse($s.value))); }
        )
        | BETWEEN (
            : b=offsetDateTimeExp AND c=offsetDateTimeExp { $exp = $a.exp.between($b.exp, $c.exp); }
            | s1=offsetDateTimeStrScalar AND s2=offsetDateTimeStrScalar { $exp = $a.exp.between($s1.value, $s2.value); }
        )
        | NOT BETWEEN (
            : b=offsetDateTimeExp AND c=offsetDateTimeExp { $exp = $a.exp.notBetween($b.exp, $c.exp); }
            | s1=offsetDateTimeStrScalar AND s2=offsetDateTimeStrScalar { $exp = $a.exp.notBetween($s1.value, $s2.value); }
        )
        | IN l=strScalarList { $exp = $a.exp.in(Arrays.stream($l.value).map(OffsetDateTime::parse).toArray(OffsetDateTime[]::new)); }
        | NOT IN l=strScalarList { $exp = $a.exp.notIn(Arrays.stream($l.value).map(OffsetDateTime::parse).toArray(OffsetDateTime[]::new)); }
    )
    ;

/**
 * A generic relational expression. Compares two expressions.
 *
 * Parameters:
 *  - The left-hand expression.
 *  - The right-hand expression.
 */
genericRelation returns [Condition exp] locals [BiFunction<Exp<?>, Exp<?>, Condition> rel]
    : a=genericExp (
        :(
            : EQ { $rel = (a, b) -> a.eq(b); }
            | NE { $rel = (a, b) -> a.ne(b); }
        ) (
            : PARAMETER { $exp = $rel.apply($a.exp, param(paramSource)); }
            | b=expression { $exp = $rel.apply($a.exp, $b.exp); }
        )
        | IN l=anyScalarList { $exp = $a.exp.in($l.value); }
        | NOT IN l=anyScalarList { $exp = $a.exp.notIn($l.value); }
    )
    ;

/// **Aggregate expressions**

/**
 * Creates an aggregating expression whose "reduce" operation returns an array containing all Series values.
 * Array component type should be provided as a fully quolified class name (e.g. 'java.lang.String').
 */
array returns [Exp<?> exp]
    : ARRAY '(' e=expression ',' t=strScalar ')' { $exp = ExpParserUtils.array($e.exp, $t.value); }
    ;

/**
 * Rule that lets the keywords of the grammar be used where an identifier is expected, e.g. as a column name.
 */
//@ doc:inline
fnName returns [String id]
    : (
    BOOL
    | INT
    | LONG
    | BIGINT
    | FLOAT
    | DOUBLE
    | DECIMAL
    | STR
    | COL
    | DATE
    | TIME
    | DATETIME
    | OFFSET_DATETIME
    | ARRAY
    | ASC
    | DESC
    ) { $id = $text; }
    ;

/// **Lexer rules**

// *General purpose tokens*

//@ doc:inline
LP: '(';

//@ doc:inline
RP: ')';

//@ doc:inline
COMMA: ',';

// *General operators*

//@ doc:inline
NOT: 'not';

//@ doc:inline
EQ: '=';

//@ doc:inline
NE: '!=';

//@ doc:inline
LE: '<=';

//@ doc:inline
GE: '>=';

//@ doc:inline
LT: '<';

//@ doc:inline
GT: '>';

//@ doc:inline
BETWEEN: 'between';

IN: 'in';

//@ doc:inline
ADD: '+';

//@ doc:inline
SUB: '-';

//@ doc:inline
MUL: '*';

//@ doc:inline
DIV: '/';

//@ doc:inline
MOD: '%';

//@ doc:inline
AND: 'and';

//@ doc:inline
OR: 'or';

// *Column operators*

//@ doc:inline
BOOL: 'bool';

//@ doc:inline
INT: 'int';

//@ doc:inline
LONG: 'long';

//@ doc:inline
BIGINT: 'bigint';

//@ doc:inline
FLOAT: 'float';

//@ doc:inline
DOUBLE: 'double';

//@ doc:inline
DECIMAL: 'decimal';

//@ doc:inline
STR: 'str';

//@ doc:inline
COL: 'col';

// *Functions*

//@ doc:inline
DATE: 'date';

//@ doc:inline
TIME: 'time';

//@ doc:inline
DATETIME: 'dateTime';

//@ doc:inline
OFFSET_DATETIME: 'offsetDateTime';

// *Aggregates*

//@ doc:inline
ARRAY: 'array';

/// *Literals*

//@ doc:inline
NULL: 'null';

//@ doc:inline
TRUE: 'true';

//@ doc:inline
FALSE: 'false';

//@ doc:inline
ASC: 'asc';

//@ doc:inline
DESC: 'desc';

//@ doc:inline
AS: 'as';

//@ doc:inline
PARAMETER: '?';

/**
 * Matches an integer literal in decimal, hexadecimal, octal, or binary format.
 */
//@ doc:inline
INTEGER_LITERAL
    : [+-]? ( DEC_LITERAL | HEX_LITERAL ) [iIlLhH]?
    ;

/**
 * Matches a floating-point literal, supporting both decimal and hexadecimal representations.
 */
//@ doc:inline
FLOAT_LITERAL
    : [+-]? ( DEC_FLOAT_LITERAL| HEX_FLOAT_LITERAL )
    ;

/**
 * Matches a string literal.
 */
STRING_LITERAL: '\'' ('\'\'' | ~['])* '\'';

/**
 * Matches a quoted identifier.
 */
QUOTED_IDENTIFIER: '`' ('``' | ~[`])* '`';

/**
 * Matches an identifier. Identifiers start with a letter and can be followed by letters or digits.
 */
IDENTIFIER: IDENTIFIER_START IDENTIFIER_PART*;

fragment DEC_LITERAL: [0-9] ([0-9_]* [0-9])?;

fragment HEX_LITERAL: '0' [xX] HEX_DIGITS;

fragment DEC_FLOAT_LITERAL
    : DEC_LITERAL DEC_EXPONENT? [fFdDmM]?
    | DEC_LITERAL? '.' DEC_LITERAL DEC_EXPONENT? [fFdDmM]?
    ;

fragment DEC_EXPONENT: [eE] [+-]? DEC_LITERAL;

fragment HEX_FLOAT_LITERAL
    : HEX_LITERAL '.'? HEX_EXPONENT [fFdDmM]?
    | '0' [xX] HEX_DIGITS? '.' HEX_DIGITS HEX_EXPONENT [fFdDmM]?
    ;

fragment HEX_EXPONENT: [pP] [+-]? DEC_LITERAL;

fragment HEX_DIGITS: [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])?;

//@ doc:inline
fragment IDENTIFIER_START: [$A-Z_a-z\u0080-\uFFFF];

//@ doc:inline
fragment IDENTIFIER_PART: [$A-Z_a-z0-9\u0080-\uFFFF];

/**
 * Skipped symbols: Whitespace and tabs.
 */
WS: [ \t\r\n]+ -> skip;
