// Generated from org/dflib/ql/antlr4/Exp.g4 by ANTLR 4.13.2
package org.dflib.ql.antlr4;

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
import org.dflib.ql.TypeClassifier;

import static org.dflib.ql.antlr4.ExpParserUtils.*;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ExpParser}.
 */
public interface ExpListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ExpParser#expRoot}.
	 * @param ctx the parse tree
	 */
	void enterExpRoot(ExpParser.ExpRootContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#expRoot}.
	 * @param ctx the parse tree
	 */
	void exitExpRoot(ExpParser.ExpRootContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#expSingle}.
	 * @param ctx the parse tree
	 */
	void enterExpSingle(ExpParser.ExpSingleContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#expSingle}.
	 * @param ctx the parse tree
	 */
	void exitExpSingle(ExpParser.ExpSingleContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#expArray}.
	 * @param ctx the parse tree
	 */
	void enterExpArray(ExpParser.ExpArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#expArray}.
	 * @param ctx the parse tree
	 */
	void exitExpArray(ExpParser.ExpArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#sorterRoot}.
	 * @param ctx the parse tree
	 */
	void enterSorterRoot(ExpParser.SorterRootContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#sorterRoot}.
	 * @param ctx the parse tree
	 */
	void exitSorterRoot(ExpParser.SorterRootContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#sorterSingle}.
	 * @param ctx the parse tree
	 */
	void enterSorterSingle(ExpParser.SorterSingleContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#sorterSingle}.
	 * @param ctx the parse tree
	 */
	void exitSorterSingle(ExpParser.SorterSingleContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#sorterArray}.
	 * @param ctx the parse tree
	 */
	void enterSorterArray(ExpParser.SorterArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#sorterArray}.
	 * @param ctx the parse tree
	 */
	void exitSorterArray(ExpParser.SorterArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ExpParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ExpParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#fnCall}.
	 * @param ctx the parse tree
	 */
	void enterFnCall(ExpParser.FnCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#fnCall}.
	 * @param ctx the parse tree
	 */
	void exitFnCall(ExpParser.FnCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#numExp}.
	 * @param ctx the parse tree
	 */
	void enterNumExp(ExpParser.NumExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#numExp}.
	 * @param ctx the parse tree
	 */
	void exitNumExp(ExpParser.NumExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#boolExp}.
	 * @param ctx the parse tree
	 */
	void enterBoolExp(ExpParser.BoolExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#boolExp}.
	 * @param ctx the parse tree
	 */
	void exitBoolExp(ExpParser.BoolExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#strExp}.
	 * @param ctx the parse tree
	 */
	void enterStrExp(ExpParser.StrExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#strExp}.
	 * @param ctx the parse tree
	 */
	void exitStrExp(ExpParser.StrExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#temporalExp}.
	 * @param ctx the parse tree
	 */
	void enterTemporalExp(ExpParser.TemporalExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#temporalExp}.
	 * @param ctx the parse tree
	 */
	void exitTemporalExp(ExpParser.TemporalExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#timeExp}.
	 * @param ctx the parse tree
	 */
	void enterTimeExp(ExpParser.TimeExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#timeExp}.
	 * @param ctx the parse tree
	 */
	void exitTimeExp(ExpParser.TimeExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateExp}.
	 * @param ctx the parse tree
	 */
	void enterDateExp(ExpParser.DateExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateExp}.
	 * @param ctx the parse tree
	 */
	void exitDateExp(ExpParser.DateExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateTimeExp}.
	 * @param ctx the parse tree
	 */
	void enterDateTimeExp(ExpParser.DateTimeExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateTimeExp}.
	 * @param ctx the parse tree
	 */
	void exitDateTimeExp(ExpParser.DateTimeExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#offsetDateTimeExp}.
	 * @param ctx the parse tree
	 */
	void enterOffsetDateTimeExp(ExpParser.OffsetDateTimeExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#offsetDateTimeExp}.
	 * @param ctx the parse tree
	 */
	void exitOffsetDateTimeExp(ExpParser.OffsetDateTimeExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#genericExp}.
	 * @param ctx the parse tree
	 */
	void enterGenericExp(ExpParser.GenericExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#genericExp}.
	 * @param ctx the parse tree
	 */
	void exitGenericExp(ExpParser.GenericExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#anyScalar}.
	 * @param ctx the parse tree
	 */
	void enterAnyScalar(ExpParser.AnyScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#anyScalar}.
	 * @param ctx the parse tree
	 */
	void exitAnyScalar(ExpParser.AnyScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#anyScalarList}.
	 * @param ctx the parse tree
	 */
	void enterAnyScalarList(ExpParser.AnyScalarListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#anyScalarList}.
	 * @param ctx the parse tree
	 */
	void exitAnyScalarList(ExpParser.AnyScalarListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#boolScalar}.
	 * @param ctx the parse tree
	 */
	void enterBoolScalar(ExpParser.BoolScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#boolScalar}.
	 * @param ctx the parse tree
	 */
	void exitBoolScalar(ExpParser.BoolScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#numScalar}.
	 * @param ctx the parse tree
	 */
	void enterNumScalar(ExpParser.NumScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#numScalar}.
	 * @param ctx the parse tree
	 */
	void exitNumScalar(ExpParser.NumScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#numScalarList}.
	 * @param ctx the parse tree
	 */
	void enterNumScalarList(ExpParser.NumScalarListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#numScalarList}.
	 * @param ctx the parse tree
	 */
	void exitNumScalarList(ExpParser.NumScalarListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#numScalarOrParamter}.
	 * @param ctx the parse tree
	 */
	void enterNumScalarOrParamter(ExpParser.NumScalarOrParamterContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#numScalarOrParamter}.
	 * @param ctx the parse tree
	 */
	void exitNumScalarOrParamter(ExpParser.NumScalarOrParamterContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#integerScalar}.
	 * @param ctx the parse tree
	 */
	void enterIntegerScalar(ExpParser.IntegerScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#integerScalar}.
	 * @param ctx the parse tree
	 */
	void exitIntegerScalar(ExpParser.IntegerScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#floatingPointScalar}.
	 * @param ctx the parse tree
	 */
	void enterFloatingPointScalar(ExpParser.FloatingPointScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#floatingPointScalar}.
	 * @param ctx the parse tree
	 */
	void exitFloatingPointScalar(ExpParser.FloatingPointScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#timeStrScalar}.
	 * @param ctx the parse tree
	 */
	void enterTimeStrScalar(ExpParser.TimeStrScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#timeStrScalar}.
	 * @param ctx the parse tree
	 */
	void exitTimeStrScalar(ExpParser.TimeStrScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateStrScalar}.
	 * @param ctx the parse tree
	 */
	void enterDateStrScalar(ExpParser.DateStrScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateStrScalar}.
	 * @param ctx the parse tree
	 */
	void exitDateStrScalar(ExpParser.DateStrScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateTimeStrScalar}.
	 * @param ctx the parse tree
	 */
	void enterDateTimeStrScalar(ExpParser.DateTimeStrScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateTimeStrScalar}.
	 * @param ctx the parse tree
	 */
	void exitDateTimeStrScalar(ExpParser.DateTimeStrScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#offsetDateTimeStrScalar}.
	 * @param ctx the parse tree
	 */
	void enterOffsetDateTimeStrScalar(ExpParser.OffsetDateTimeStrScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#offsetDateTimeStrScalar}.
	 * @param ctx the parse tree
	 */
	void exitOffsetDateTimeStrScalar(ExpParser.OffsetDateTimeStrScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#strScalar}.
	 * @param ctx the parse tree
	 */
	void enterStrScalar(ExpParser.StrScalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#strScalar}.
	 * @param ctx the parse tree
	 */
	void exitStrScalar(ExpParser.StrScalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#strScalarList}.
	 * @param ctx the parse tree
	 */
	void enterStrScalarList(ExpParser.StrScalarListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#strScalarList}.
	 * @param ctx the parse tree
	 */
	void exitStrScalarList(ExpParser.StrScalarListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#strScalarOrParameter}.
	 * @param ctx the parse tree
	 */
	void enterStrScalarOrParameter(ExpParser.StrScalarOrParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#strScalarOrParameter}.
	 * @param ctx the parse tree
	 */
	void exitStrScalarOrParameter(ExpParser.StrScalarOrParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#numColumn}.
	 * @param ctx the parse tree
	 */
	void enterNumColumn(ExpParser.NumColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#numColumn}.
	 * @param ctx the parse tree
	 */
	void exitNumColumn(ExpParser.NumColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#intColumn}.
	 * @param ctx the parse tree
	 */
	void enterIntColumn(ExpParser.IntColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#intColumn}.
	 * @param ctx the parse tree
	 */
	void exitIntColumn(ExpParser.IntColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#longColumn}.
	 * @param ctx the parse tree
	 */
	void enterLongColumn(ExpParser.LongColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#longColumn}.
	 * @param ctx the parse tree
	 */
	void exitLongColumn(ExpParser.LongColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#bigintColumn}.
	 * @param ctx the parse tree
	 */
	void enterBigintColumn(ExpParser.BigintColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#bigintColumn}.
	 * @param ctx the parse tree
	 */
	void exitBigintColumn(ExpParser.BigintColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#floatColumn}.
	 * @param ctx the parse tree
	 */
	void enterFloatColumn(ExpParser.FloatColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#floatColumn}.
	 * @param ctx the parse tree
	 */
	void exitFloatColumn(ExpParser.FloatColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#doubleColumn}.
	 * @param ctx the parse tree
	 */
	void enterDoubleColumn(ExpParser.DoubleColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#doubleColumn}.
	 * @param ctx the parse tree
	 */
	void exitDoubleColumn(ExpParser.DoubleColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#decimalColumn}.
	 * @param ctx the parse tree
	 */
	void enterDecimalColumn(ExpParser.DecimalColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#decimalColumn}.
	 * @param ctx the parse tree
	 */
	void exitDecimalColumn(ExpParser.DecimalColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#boolColumn}.
	 * @param ctx the parse tree
	 */
	void enterBoolColumn(ExpParser.BoolColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#boolColumn}.
	 * @param ctx the parse tree
	 */
	void exitBoolColumn(ExpParser.BoolColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#strColumn}.
	 * @param ctx the parse tree
	 */
	void enterStrColumn(ExpParser.StrColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#strColumn}.
	 * @param ctx the parse tree
	 */
	void exitStrColumn(ExpParser.StrColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateColumn}.
	 * @param ctx the parse tree
	 */
	void enterDateColumn(ExpParser.DateColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateColumn}.
	 * @param ctx the parse tree
	 */
	void exitDateColumn(ExpParser.DateColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#timeColumn}.
	 * @param ctx the parse tree
	 */
	void enterTimeColumn(ExpParser.TimeColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#timeColumn}.
	 * @param ctx the parse tree
	 */
	void exitTimeColumn(ExpParser.TimeColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateTimeColumn}.
	 * @param ctx the parse tree
	 */
	void enterDateTimeColumn(ExpParser.DateTimeColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateTimeColumn}.
	 * @param ctx the parse tree
	 */
	void exitDateTimeColumn(ExpParser.DateTimeColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#offsetDateTimeColumn}.
	 * @param ctx the parse tree
	 */
	void enterOffsetDateTimeColumn(ExpParser.OffsetDateTimeColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#offsetDateTimeColumn}.
	 * @param ctx the parse tree
	 */
	void exitOffsetDateTimeColumn(ExpParser.OffsetDateTimeColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#genericColumn}.
	 * @param ctx the parse tree
	 */
	void enterGenericColumn(ExpParser.GenericColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#genericColumn}.
	 * @param ctx the parse tree
	 */
	void exitGenericColumn(ExpParser.GenericColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#columnId}.
	 * @param ctx the parse tree
	 */
	void enterColumnId(ExpParser.ColumnIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#columnId}.
	 * @param ctx the parse tree
	 */
	void exitColumnId(ExpParser.ColumnIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ExpParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ExpParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelation(ExpParser.RelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelation(ExpParser.RelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#fnRelation}.
	 * @param ctx the parse tree
	 */
	void enterFnRelation(ExpParser.FnRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#fnRelation}.
	 * @param ctx the parse tree
	 */
	void exitFnRelation(ExpParser.FnRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#numRelation}.
	 * @param ctx the parse tree
	 */
	void enterNumRelation(ExpParser.NumRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#numRelation}.
	 * @param ctx the parse tree
	 */
	void exitNumRelation(ExpParser.NumRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#strRelation}.
	 * @param ctx the parse tree
	 */
	void enterStrRelation(ExpParser.StrRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#strRelation}.
	 * @param ctx the parse tree
	 */
	void exitStrRelation(ExpParser.StrRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#timeRelation}.
	 * @param ctx the parse tree
	 */
	void enterTimeRelation(ExpParser.TimeRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#timeRelation}.
	 * @param ctx the parse tree
	 */
	void exitTimeRelation(ExpParser.TimeRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateRelation}.
	 * @param ctx the parse tree
	 */
	void enterDateRelation(ExpParser.DateRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateRelation}.
	 * @param ctx the parse tree
	 */
	void exitDateRelation(ExpParser.DateRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#dateTimeRelation}.
	 * @param ctx the parse tree
	 */
	void enterDateTimeRelation(ExpParser.DateTimeRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#dateTimeRelation}.
	 * @param ctx the parse tree
	 */
	void exitDateTimeRelation(ExpParser.DateTimeRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#offsetDateTimeRelation}.
	 * @param ctx the parse tree
	 */
	void enterOffsetDateTimeRelation(ExpParser.OffsetDateTimeRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#offsetDateTimeRelation}.
	 * @param ctx the parse tree
	 */
	void exitOffsetDateTimeRelation(ExpParser.OffsetDateTimeRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#genericRelation}.
	 * @param ctx the parse tree
	 */
	void enterGenericRelation(ExpParser.GenericRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#genericRelation}.
	 * @param ctx the parse tree
	 */
	void exitGenericRelation(ExpParser.GenericRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#array}.
	 * @param ctx the parse tree
	 */
	void enterArray(ExpParser.ArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#array}.
	 * @param ctx the parse tree
	 */
	void exitArray(ExpParser.ArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpParser#fnName}.
	 * @param ctx the parse tree
	 */
	void enterFnName(ExpParser.FnNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpParser#fnName}.
	 * @param ctx the parse tree
	 */
	void exitFnName(ExpParser.FnNameContext ctx);
}