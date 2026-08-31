package org.dflib.ql;

import org.dflib.*;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.ANY;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.BOOLEAN;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.DATE;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.DATETIME;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NUMERIC;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.OBJECT;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.OFFSETDATETIME;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.STRING;
import static org.junit.jupiter.api.Assertions.*;

class QLFunctionsTest {

    private static Arg arg(TypeClassifier type) {
        return new Arg(type, false);
    }

    private static Arg constant(TypeClassifier type) {
        return new Arg(type, true);
    }

    @Test
    void mayReturn_ScopedByReturnType() {

        // the grammar picks the expression rule to parse a call with by asking this predicate about the name
        // alone, so it must answer only for functions of the type being asked about
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .function("trim", new ObjectArgFunction())
                .function("isTrue", new BoolFunction())
                .function("split", new ArrayFunction())
                .build();

        assertTrue(functions.mayReturn("sum", NUMERIC));
        assertFalse(functions.mayReturn("sum", STRING));
        assertFalse(functions.mayReturn("sum", BOOLEAN));
        assertFalse(functions.mayReturn("sum", OBJECT));

        assertTrue(functions.mayReturn("trim", STRING));
        assertFalse(functions.mayReturn("trim", NUMERIC));

        assertTrue(functions.mayReturn("isTrue", BOOLEAN));
        assertFalse(functions.mayReturn("isTrue", OBJECT));

        // an array-valued function has no dedicated expression type, so it is reachable only as an OBJECT one
        assertTrue(functions.mayReturn("split", OBJECT));
        assertFalse(functions.mayReturn("split", STRING));

        for (TypeClassifier t : TypeClassifier.values()) {
            assertFalse(functions.mayReturn("unknown", t), t.name());
        }
    }

    @Test
    void mayReturn_TemporalTypesAreDistinct() {

        // LocalDateTime and OffsetDateTime are separate expression types in the grammar, and must not share a
        // classifier - otherwise a function of one would be looked up in the rule of the other
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("asDate", new DateFunction())
                .function("asOffsetDateTime", new OffsetDateTimeFunction())
                .build();

        assertTrue(functions.mayReturn("asDate", DATE));
        assertFalse(functions.mayReturn("asDate", DATETIME));
        assertFalse(functions.mayReturn("asDate", OFFSETDATETIME));

        assertTrue(functions.mayReturn("asOffsetDateTime", OFFSETDATETIME));
        assertFalse(functions.mayReturn("asOffsetDateTime", DATETIME));
    }

    @Test
    void function_Matching2ArgTypes() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .function("sum", new Int3SumFunction())
                .build();

        QLFunctionDescriptor result = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC)));

        assertNotNull(result);
        assertEquals("sum", result.name());
        assertEquals(NUMERIC, result.returnType());
        assertEquals(2, result.args().length);
    }

    @Test
    void function_Matching3ArgTypes() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .function("sum", new Int3SumFunction())
                .build();

        QLFunctionDescriptor result = functions.function("sum",
                List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));

        assertNotNull(result);
        assertEquals("sum", result.name());
        assertEquals(NUMERIC, result.returnType());
        assertEquals(3, result.args().length);
    }

    @Test
    void function_WrongArgTypes() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .build();
        List<Arg> wrongArgTypes = List.of(arg(NUMERIC), arg(STRING));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("sum", wrongArgTypes)
        );
        assertEquals("Function sum([NUMERIC, STRING]) not found", exception.getMessage());
    }

    @Test
    void function_ConstantArg_RenderedInNotFoundMessage() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("substr", new StrConstIntFunction())
                .build();

        // a plain argument renders as the bare type name, a constant one is called out
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("substr", List.of(arg(STRING), constant(STRING)))
        );
        assertEquals("Function substr([STRING, const STRING]) not found", exception.getMessage());
    }

    @Test
    void function_VarArgs_MatchesAnyArity() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new IntNSumFunction())
                .build();

        // match with 2 args
        QLFunctionDescriptor result2 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result2);
        assertTrue(result2.isVarArgs());

        // match with 3 args
        QLFunctionDescriptor result3 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result3);
        assertTrue(result3.isVarArgs());

        // match with 1 arg
        QLFunctionDescriptor result1 = functions.function("sum", List.of(arg(NUMERIC)));
        assertNotNull(result1);
        assertTrue(result1.isVarArgs());

        // match with 0 args
        QLFunctionDescriptor result0 = functions.function("sum", List.of());
        assertNotNull(result0);
        assertTrue(result0.isVarArgs());
    }

    @Test
    void function_VarArgs_FixedArityPreferred() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .function("sum", new IntNSumFunction())
                .build();

        // 2-arg call should prefer the fixed-arity Udf2
        QLFunctionDescriptor result2 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result2);
        assertFalse(result2.isVarArgs());
        assertEquals(2, result2.args().length);

        // 3-arg call should fall back to varargs
        QLFunctionDescriptor result3 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result3);
        assertTrue(result3.isVarArgs());
    }

    @Test
    void function_AnyArgType_MatchesTypedParam() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .build();

        // ANY is the classification of an argument whose type is only known at eval time. It must be accepted by a
        // parameter of any declared type
        QLFunctionDescriptor result = functions.function("sum", List.of(arg(ANY), arg(ANY)));

        assertNotNull(result);
        assertEquals(2, result.args().length);
    }

    @Test
    void function_ObjectArgType_DoesNotMatchTypedParam() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .build();

        // unlike ANY, OBJECT is a deliberately Object-typed argument (a null literal, most notably), and must not
        // be silently passed to a numeric parameter
        List<Arg> argTypes = List.of(arg(NUMERIC), arg(OBJECT));

        assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("sum", argTypes)
        );
    }

    @Test
    void function_ExactMatchPreferredOverWildcard() {

        // the wildcard overload is registered first, so specificity rather than registration order has to decide
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", new ObjectArgFunction())
                .function("f", new StrArgFunction())
                .build();

        QLFunctionDescriptor result = functions.function("f", List.of(arg(STRING)));

        assertArrayEquals(new Arg[]{arg(STRING)}, result.args());
    }

    @Test
    void function_AnyArg_PrefersWildcardParam() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", new StrArgFunction())
                .function("f", new ObjectArgFunction())
                .build();

        // an ANY argument matches both overloads, but only the one declaring OBJECT is written to handle an
        // argument of any type, so it is the more specific match
        QLFunctionDescriptor result = functions.function("f", List.of(arg(ANY)));

        assertArrayEquals(new Arg[]{arg(OBJECT)}, result.args());
    }

    @Test
    void function_EquallySpecific_FirstRegisteredWins() {

        // neither overload is more specific than the other: each matches one argument exactly and the other via a
        // wildcard. The tie must resolve to whichever was registered first
        List<Arg> argTypes = List.of(arg(STRING), arg(STRING));

        QLFunctions strFirst = QLFunctions.builder().noDefaultFunctions()
                .function("f", new StrObjArgFunction())
                .function("f", new ObjStrArgFunction())
                .build();

        assertArrayEquals(
                new Arg[]{arg(STRING), arg(OBJECT)},
                strFirst.function("f", argTypes).args());

        QLFunctions objFirst = QLFunctions.builder().noDefaultFunctions()
                .function("f", new ObjStrArgFunction())
                .function("f", new StrObjArgFunction())
                .build();

        assertArrayEquals(
                new Arg[]{arg(OBJECT), arg(STRING)},
                objFirst.function("f", argTypes).args());
    }

    // --- ambiguity caused by an argument whose type is only known at eval time ---

    /**
     * Two overloads that differ only in the type of one parameter, e.g. "avg(NumExp)" and "avg(DateExp)".
     */
    private static QLFunctions.Builder twoReceivers(TypeClassifier a, TypeClassifier b) {
        return QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature().returning(STRING).arg(a).as(args -> args.get(0).castAsStr()))
                .function("f", QLFunctionSignature.signature().returning(STRING).arg(b).as(args -> args.get(0).castAsStr()));
    }

    @Test
    void function_AnyArg_AmbiguousAmongTypedOverloads() {

        // an untyped argument matches both overloads at the same cost, so registration order would decide which
        // receiver type the call is compiled for. That is a coin toss over the user's data, not a resolution
        QLFunctions functions = twoReceivers(NUMERIC, DATE).build();

        assertNotNull(functions.function("f", List.of(arg(NUMERIC))));
        assertNotNull(functions.function("f", List.of(arg(DATE))));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("f", List.of(arg(ANY))));

        assertEquals("Ambiguous call to f(): the type of argument 1 is only known at eval time,"
                + " and f is defined for [NUMERIC, DATE] arguments in that position."
                + " Cast it, e.g. f(castAsInt(..))", e.getMessage());
    }

    @Test
    void function_AnyArg_AmbiguityReportedAtItsOwnPosition() {

        // the ambiguous position is not necessarily the first one, and the hint names one of the types actually
        // declared there
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(BOOLEAN).arg(DATE).as(args -> args.get(0).castAsStr()))
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(BOOLEAN).arg(STRING).as(args -> args.get(0).castAsStr()))
                .build();

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("f", List.of(arg(BOOLEAN), arg(ANY))));

        assertEquals("Ambiguous call to f(): the type of argument 2 is only known at eval time,"
                + " and f is defined for [STRING, DATE] arguments in that position."
                + " Cast it, e.g. f(castAsStr(..))", e.getMessage());
    }

    @Test
    void function_AnyArg_AmbiguityIsPerArgument() {

        // only the untyped argument is ambiguous - the same overloads resolve fine as soon as its type is known
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(NUMERIC).arg(BOOLEAN).as(args -> args.get(0).castAsStr()))
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(DATE).arg(BOOLEAN).as(args -> args.get(0).castAsStr()))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("f", List.of(arg(ANY), arg(BOOLEAN))));

        assertArrayEquals(
                new Arg[]{arg(NUMERIC), arg(BOOLEAN)},
                functions.function("f", List.of(arg(NUMERIC), arg(BOOLEAN))).args());

        assertArrayEquals(
                new Arg[]{arg(DATE), arg(BOOLEAN)},
                functions.function("f", List.of(arg(DATE), arg(BOOLEAN))).args());
    }

    @Test
    void function_AnyArg_TieAwayFromTheAnyPositionIsNotAmbiguous() {

        // the two overloads tie, but not because of the untyped argument: they agree on its declared type and
        // differ only where the caller did provide a type. Registration order remains the right answer
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(DATE).arg(STRING).arg(OBJECT).as(args -> args.get(0).castAsStr()))
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(DATE).arg(OBJECT).arg(STRING).as(args -> args.get(0).castAsStr()))
                .build();

        assertArrayEquals(
                new Arg[]{arg(DATE), arg(STRING), arg(OBJECT)},
                functions.function("f", List.of(arg(ANY), arg(STRING), arg(STRING))).args());
    }

    @Test
    void function_AnyArg_WildcardOverloadResolvesTheAmbiguity() {

        // "shift(a, 2)": an overload written to accept an argument of any type is strictly the better match for an
        // untyped argument, so it wins outright and there is nothing to be ambiguous about
        QLFunctions functions = twoReceivers(NUMERIC, DATE)
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(OBJECT).as(args -> args.get(0).castAsStr()))
                .build();

        assertArrayEquals(new Arg[]{arg(OBJECT)}, functions.function("f", List.of(arg(ANY))).args());
    }

    @Test
    void function_AnyArg_WildcardOverloadWinsATie() {

        // "shift(a, 1, 'x')": the wildcard overload pays for the second argument what the typed one pays for the
        // untyped first, so the per-argument costs add up to a tie. It is still not an ambiguity - only the
        // wildcard overload is written to accept a receiver of any type, and the typed one would reject it
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(STRING).arg(STRING).as(args -> args.get(0).castAsStr()))
                .function("f", QLFunctionSignature.signature()
                        .returning(OBJECT).arg(OBJECT).arg(OBJECT).as(args -> args.get(0).castAsStr()))
                .build();

        assertArrayEquals(
                new Arg[]{arg(OBJECT), arg(OBJECT)},
                functions.function("f", List.of(arg(ANY), arg(STRING))).args());

        // a typed argument in that position still picks the typed overload
        assertArrayEquals(
                new Arg[]{arg(STRING), arg(STRING)},
                functions.function("f", List.of(arg(STRING), arg(STRING))).args());
    }

    @Test
    void function_AnyArg_FixedArityWinsOverVarArgs() {

        // a vararg overload never ties with a fixed-arity one, so an untyped argument can not make them ambiguous
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(NUMERIC).as(args -> args.get(0).castAsStr()))
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).varArgs().as(args -> args.get(0).castAsStr()))
                .build();

        QLFunctionDescriptor descriptor = functions.function("f", List.of(arg(ANY)));

        assertFalse(descriptor.isVarArgs());
        assertArrayEquals(new Arg[]{arg(NUMERIC)}, descriptor.args());
    }

    @Test
    void function_AnyArg_VarArgsAmbiguousOnALeadingParam() {

        // vararg overloads are compared by their leading typed parameters only. The first argument is ambiguous;
        // the second one is untyped too, but lands past everything either overload declares
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(NUMERIC).varArgs().as(args -> args.get(0).castAsStr()))
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING).arg(DATE).varArgs().as(args -> args.get(0).castAsStr()))
                .build();

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("f", List.of(arg(ANY), arg(ANY))));

        assertEquals("Ambiguous call to f(): the type of argument 1 is only known at eval time,"
                + " and f is defined for [NUMERIC, DATE] arguments in that position."
                + " Cast it, e.g. f(castAsInt(..))", e.getMessage());
    }

    @Test
    void function_AnyArg_NotFoundStillReportsNotFound() {

        // an ambiguity is only possible among matching overloads. When none match, the message stays the one the
        // parser reports for an unknown call
        QLFunctions functions = twoReceivers(NUMERIC, DATE).build();

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("f", List.of(arg(ANY), arg(ANY))));

        assertEquals("Function f([ANY, ANY]) not found", e.getMessage());
    }

    @Test
    void function_ConstantParam_MatchesConstantArg() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("substr", new StrConstIntFunction())
                .build();

        QLFunctionDescriptor result = functions.function("substr", List.of(arg(STRING), constant(NUMERIC)));

        assertNotNull(result);
        assertArrayEquals(new Arg[]{arg(OBJECT), constant(NUMERIC)}, result.args());
    }

    @Test
    void function_ConstantParam_RejectsNonConstantArg() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("substr", new StrConstIntFunction())
                .build();

        // a column reference is a numeric expression, so it matches on type, but its value is not known until eval
        List<Arg> argTypes = List.of(arg(STRING), arg(NUMERIC));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("substr", argTypes)
        );
        assertEquals("Function substr([STRING, NUMERIC]) not found", exception.getMessage());
    }

    @Test
    void function_ConstantParam_UntypedArgStillRejected() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("substr", new StrConstIntFunction())
                .build();

        // ANY is compatible with any parameter type, but it is by definition not a constant
        assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("substr", List.of(arg(STRING), arg(ANY)))
        );
    }

    @Test
    void function_PlainParam_AcceptsConstantArg() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .build();

        // passing a literal to an unconstrained parameter stays legal - this is what "abs(-5)" does
        QLFunctionDescriptor result = functions.function("sum", List.of(constant(NUMERIC), constant(NUMERIC)));

        assertNotNull(result);
    }

    @Test
    void function_ConstancyDistinguishesOverloads() {

        // the same signature with a different constancy is a different function, and both must register
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("substr", new StrConstIntFunction())
                .function("substr", new StrIntFunction())
                .build();

        assertArrayEquals(
                new Arg[]{arg(OBJECT), constant(NUMERIC)},
                functions.function("substr", List.of(arg(STRING), constant(NUMERIC))).args());

        assertArrayEquals(
                new Arg[]{arg(OBJECT), arg(NUMERIC)},
                functions.function("substr", List.of(arg(STRING), arg(NUMERIC))).args());
    }

    @Test
    void function_SameFunctionIsADuplicate() {
        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions().function("substr", new StrConstIntFunction());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.function("substr", new StrConstIntFunction())
        );
        assertTrue(exception.getMessage().contains("already defined"), exception.getMessage());
    }

    @Test
    void constantArg_OnVarArgsIsRejected() {

        // varargs declare no individual arguments, so a constant marker on them would be silently dropped
        assertThrows(
                IllegalArgumentException.class,
                () -> QLFunctions.builder().noDefaultFunctions().function("sum", new ConstVarArgsFunction())
        );
    }

    // --- single-hook resolution API (name -> descriptor, no return type filter) ---

    @Test
    void isFn() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions().function("sum", new Int2SumFunction()).build();

        assertTrue(functions.isFn("sum"));
        assertFalse(functions.isFn("unknown"));
    }

    @Test
    void mayReturn_FixedReturnType() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .function("trim", new ObjectArgFunction())
                .build();

        assertTrue(functions.mayReturn("sum", NUMERIC));
        assertFalse(functions.mayReturn("sum", STRING));
        assertTrue(functions.mayReturn("trim", STRING));
        assertFalse(functions.mayReturn("unknown", NUMERIC));
    }

    @Test
    void mayReturn_OverloadsWithDifferentReturnTypes() {

        // "mayReturn" is a per-name over-approximation: it must answer for the union of the name's overloads
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", new ObjectArgFunction())
                .function("f", new Int2SumFunction())
                .build();

        assertTrue(functions.mayReturn("f", STRING));
        assertTrue(functions.mayReturn("f", NUMERIC));
        assertFalse(functions.mayReturn("f", BOOLEAN));
    }

    @Test
    void mayReturn_Polymorphic() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("shift", QLFunctionSignature.signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0)))
                .build();

        // a function returning the type of its argument may return any concrete type, but never ANY: ANY is the
        // absence of a type and no typed expression rule can consume it
        for (TypeClassifier t : TypeClassifier.values()) {
            assertEquals(t != ANY, functions.mayReturn("shift", t), t.name());
        }
    }

    @Test
    void mayReturn_AnyReturningFunction() {

        // "first"/"if"/"ifNull" produce expressions that implement no typed interface, so no typed rule may claim
        // them. They stay reachable from the untyped expression position, which never asks "mayReturn"
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("first", QLFunctionSignature.signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .as(args -> args.get(0).first()))
                .build();

        assertTrue(functions.isFn("first"));
        assertTrue(functions.mayReturn("first", ANY));
        assertFalse(functions.mayReturn("first", NUMERIC));
        assertFalse(functions.mayReturn("first", STRING));
        assertFalse(functions.mayReturn("first", OBJECT));
    }

    @Test
    void hasTypedReturn() {

        // the question the grammar asks to decide whether a call site belongs to a typed rule or to the untyped
        // expression position: it is the union of "mayReturn" over the types that have a rule of their own
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .function("split", QLFunctionSignature.signature()
                        .returning(OBJECT)
                        .arg(OBJECT)
                        .as(args -> args.get(0).list()))
                .function("first", QLFunctionSignature.signature()
                        .returning(ANY)
                        .arg(OBJECT)
                        .as(args -> args.get(0).first()))
                .function("shift", QLFunctionSignature.signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0)))
                .build();

        assertTrue(functions.hasTypedReturn("sum"));

        // a polymorphic function may return any concrete type, so every typed rule claims it
        assertTrue(functions.hasTypedReturn("shift"));

        // neither a plain object nor a type known only at eval time has a rule of its own
        assertFalse(functions.hasTypedReturn("split"));
        assertFalse(functions.hasTypedReturn("first"));

        assertFalse(functions.hasTypedReturn("unknown"));
    }

    @Test
    void isPolymorphicFn() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                // a single fixed return type
                .function("trim", new ObjectArgFunction())
                // two overloads, same fixed return type
                .function("sum", new Int2SumFunction())
                .function("sum", new Int3SumFunction())
                // two overloads with different fixed return types
                .function("f", new ObjectArgFunction())
                .function("f", new Int2SumFunction())
                // returns the type of its argument
                .function("shift", QLFunctionSignature.signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .as(args -> args.get(0)))
                .build();

        assertFalse(functions.isPolymorphicFn("trim"));
        assertFalse(functions.isPolymorphicFn("sum"));
        assertTrue(functions.isPolymorphicFn("f"));
        assertTrue(functions.isPolymorphicFn("shift"));
        assertFalse(functions.isPolymorphicFn("unknown"));
    }

    @Test
    void functionByName_IgnoresReturnType() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", new ObjectArgFunction())
                .function("f", new Int2SumFunction())
                .build();

        assertEquals(STRING, functions.function("f", List.of(arg(STRING))).returnType());
        assertEquals(NUMERIC, functions.function("f", List.of(arg(NUMERIC), arg(NUMERIC))).returnType());
    }

    @Test
    void functionByName_NotFound() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions().function("sum", new Int2SumFunction()).build();
        List<Arg> argTypes = List.of(arg(NUMERIC), arg(NUMERIC));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("multiply", argTypes)
        );
        assertEquals("Function multiply([NUMERIC, NUMERIC]) not found", exception.getMessage());
    }

    @Test
    void functionByName_TypedExpressionWithoutATypedInterface() {

        // "year(first(date(a)))": the argument is a FirstExp<LocalDate>, which implements no typed Exp interface.
        // It classifies as ANY and so must still be passable to a DATE parameter
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("year", QLFunctionSignature.signature()
                        .returning(NUMERIC)
                        .arg(DATE)
                        .as(args -> ((DateExp) args.get(0)).year()))
                .build();

        QLFunctionDescriptor descriptor = functions.function("year", List.of(Arg.of(Exp.$date("a").first())));

        assertNotNull(descriptor);
        assertEquals(NUMERIC, descriptor.returnType());
    }

    // --- explicit signature registration ---

    @Test
    void signature_FourArgs() {

        // reflection tops out at Udf3. An explicit signature has no arity ceiling
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("between", QLFunctionSignature.signature()
                        .returning(BOOLEAN)
                        .arg(NUMERIC)
                        .arg(NUMERIC)
                        .arg(NUMERIC)
                        .constArg(STRING)
                        .as(args -> args.get(0).castAsBool()))
                .build();

        QLFunctionDescriptor descriptor = functions.function("between",
                List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC), constant(STRING)));

        assertEquals(4, descriptor.args().length);
        assertEquals(BOOLEAN, descriptor.returnType());

        assertThrows(IllegalArgumentException.class, () -> functions.function("between",
                List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC))));
    }

    @Test
    void signature_ZeroArgOverloadBesideVarArgs() {

        // "concat()" is a zero-arity overload registered next to the varargs one. Fixed arity is preferred, so the
        // empty call resolves to it rather than to a zero-length vararg list
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("concat", QLFunctionSignature.signature()
                        .returning(STRING)
                        .varArgs()
                        .as(args -> args.get(0).castAsStr()))
                .function("concat", QLFunctionSignature.signature()
                        .returning(STRING)
                        .as(args -> Exp.$strVal("")))
                .build();

        assertFalse(functions.function("concat", List.of()).isVarArgs());
        assertTrue(functions.function("concat", List.of(arg(STRING))).isVarArgs());
        assertTrue(functions.function("concat", List.of(arg(STRING), arg(NUMERIC))).isVarArgs());
    }

    @Test
    void signature_VarArgsWithLeadingTypedParams() {

        // declared args of a vararg function are leading typed parameters: they have to be present and to match,
        // everything past them is unconstrained
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("vConcat", QLFunctionSignature.signature()
                        .returning(STRING)
                        .constArg(STRING)
                        .varArgs()
                        .as(args -> args.get(1).castAsStr()))
                .build();

        assertNotNull(functions.function("vConcat", List.of(constant(STRING), arg(NUMERIC), arg(DATE))));
        assertNotNull(functions.function("vConcat", List.of(constant(STRING))));

        // the leading parameter is missing
        assertThrows(IllegalArgumentException.class, () -> functions.function("vConcat", List.of()));

        // ... or is not a constant
        assertThrows(IllegalArgumentException.class,
                () -> functions.function("vConcat", List.of(arg(STRING), arg(STRING))));

        // ... or is of the wrong type
        assertThrows(IllegalArgumentException.class,
                () -> functions.function("vConcat", List.of(constant(NUMERIC), arg(STRING))));
    }

    @Test
    void signature_ConstArgNeedsNoAnnotation() {

        // the explicit path declares constancy directly, without a @Constant annotation to reflect on
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("substr", QLFunctionSignature.signature()
                        .returning(STRING)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0).castAsStr()))
                .build();

        assertArrayEquals(
                new Arg[]{arg(OBJECT), constant(NUMERIC)},
                functions.function("substr", List.of(arg(STRING), constant(NUMERIC))).args());

        assertThrows(IllegalArgumentException.class,
                () -> functions.function("substr", List.of(arg(STRING), arg(NUMERIC))));
    }

    @Test
    void signature_ReturningArgType() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("shift", QLFunctionSignature.signature()
                        .returningArgType(0)
                        .arg(OBJECT)
                        .constArg(NUMERIC)
                        .as(args -> args.get(0)))
                .build();

        List<Arg> dateArgs = List.of(arg(DATE), constant(NUMERIC));
        QLFunctionDescriptor descriptor = functions.function("shift", dateArgs);

        assertNull(descriptor.returnType());
        assertEquals(0, descriptor.returnArgIndex());
        assertEquals(DATE, descriptor.returnType(dateArgs));
        assertEquals(STRING, descriptor.returnType(List.of(arg(STRING), constant(NUMERIC))));
        assertEquals(ANY, descriptor.returnType(List.of(arg(ANY), constant(NUMERIC))));
    }

    @Test
    void signature_DuplicateShapeRejected() {

        // the return type is not part of a descriptor's identity: two overloads with the same argument shape are
        // unresolvable regardless of what they return, and must fail at build time
        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returning(STRING)
                        .arg(NUMERIC)
                        .as(args -> args.get(0).castAsStr()));

        assertThrows(IllegalArgumentException.class, () -> builder
                .function("f", QLFunctionSignature.signature()
                        .returning(BOOLEAN)
                        .arg(NUMERIC)
                        .as(args -> args.get(0).castAsBool())));
    }

    @Test
    void signature_ProducerRequired() {
        assertThrows(IllegalArgumentException.class, () -> QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature().returning(STRING).arg(NUMERIC)));
    }

    @Test
    void signature_ReturnTypeRequired() {
        assertThrows(IllegalArgumentException.class, () -> QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature().arg(NUMERIC).as(args -> args.get(0))));
    }

    @Test
    void signature_ReturnArgIndexOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> QLFunctions.builder().noDefaultFunctions()
                .function("f", QLFunctionSignature.signature()
                        .returningArgType(1)
                        .arg(NUMERIC)
                        .as(args -> args.get(0))));
    }

    private static class BoolFunction implements Udf1<Object, Boolean> {
        @Override
        public Condition call(Exp<Object> exp) {
            return exp.castAsBool();
        }
    }

    private static class ArrayFunction implements Udf1<String, String[]> {
        @Override
        public Exp<String[]> call(Exp<String> exp) {
            return exp.castAsStr().split(',');
        }
    }

    private static class DateFunction implements Udf1<Object, LocalDate> {
        @Override
        public DateExp call(Exp<Object> exp) {
            return exp.castAsDate();
        }
    }

    private static class OffsetDateTimeFunction implements Udf1<Object, OffsetDateTime> {
        @Override
        public OffsetDateTimeExp call(Exp<Object> exp) {
            return exp.castAsOffsetDateTime();
        }
    }

    private static class StrConstIntFunction implements Udf2<Object, Integer, String> {
        @Override
        public Exp<String> call(Exp<Object> exp, @Constant Exp<Integer> from) {
            return exp.substr(from.reduce((Series<?>) null));
        }
    }

    /**
     * Same erased signature as {@link StrConstIntFunction}, but taking any expression as the second argument.
     */
    private static class StrIntFunction implements Udf2<Object, Integer, String> {
        @Override
        public Exp<String> call(Exp<Object> exp, Exp<Integer> from) {
            return exp.castAsStr();
        }
    }

    private static class ConstVarArgsFunction implements UdfN<Number> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public NumExp<Number> call(@Constant Exp<?>... exps) {
            return (NumExp) exps[0];
        }
    }

    private static class StrObjArgFunction implements Udf2<String, Object, String> {
        @Override
        public Exp<String> call(Exp<String> a, Exp<Object> b) {
            return Exp.concat(a, b);
        }
    }

    private static class ObjStrArgFunction implements Udf2<Object, String, String> {
        @Override
        public Exp<String> call(Exp<Object> a, Exp<String> b) {
            return Exp.concat(a, b);
        }
    }

    private static class StrArgFunction implements Udf1<String, String> {
        @Override
        public Exp<String> call(Exp<String> exp) {
            return exp.castAsStr().trim();
        }
    }

    private static class ObjectArgFunction implements Udf1<Object, String> {
        @Override
        public Exp<String> call(Exp<Object> exp) {
            return exp.trim();
        }
    }

    private static class IntNSumFunction implements UdfN<Number> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public NumExp<Number> call(Exp<?>... exps) {
            NumExp<Number> result = (NumExp) exps[0];
            for (int i = 1; i < exps.length; i++) {
                result = result.add((NumExp) exps[i]);
            }
            return result;
        }
    }

    private static class Int2SumFunction implements Udf2<Integer, Integer, Integer> {
        @Override
        public Exp<Integer> call(Exp<Integer> a, Exp<Integer> b) {
            return a.castAsInt().add(b.castAsInt()).castAsInt();
        }
    }

    private static class Int3SumFunction implements Udf3<Integer, Integer, Integer, Integer> {
        @Override
        public Exp<Integer> call(Exp<Integer> a, Exp<Integer> b, Exp<Integer> c) {
            return a.castAsInt().add(b.castAsInt()).castAsInt().add(c.castAsInt()).castAsInt();
        }
    }
}
