package org.dflib.ql;

import org.dflib.*;
import org.dflib.exp.fn.Constant;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.ANY;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NUMERIC;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.OBJECT;
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
    void returnTypePredicates_ScopedByReturnType() {

        // the grammar picks the expression rule to parse a call with by asking these predicates about the name
        // alone, so each must answer only for functions of its own return type
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .function("trim", new ObjectArgFunction())
                .function("isTrue", new BoolFunction())
                .function("split", new ArrayFunction())
                .build();

        assertTrue(functions.numFn("sum"));
        assertFalse(functions.strFn("sum"));
        assertFalse(functions.boolFn("sum"));
        assertFalse(functions.objectFn("sum"));

        assertTrue(functions.strFn("trim"));
        assertFalse(functions.numFn("trim"));

        assertTrue(functions.boolFn("isTrue"));
        assertFalse(functions.objectFn("isTrue"));

        // an array-valued function has no dedicated expression type, so it is reachable only as an OBJECT one
        assertTrue(functions.objectFn("split"));
        assertFalse(functions.strFn("split"));

        assertFalse(functions.numFn("unknown"));
        assertFalse(functions.boolFn("unknown"));
        assertFalse(functions.objectFn("unknown"));
        assertFalse(functions.timeFn("unknown"));
        assertFalse(functions.dateFn("unknown"));
        assertFalse(functions.dateTimeFn("unknown"));
        assertFalse(functions.offsetDateTimeFn("unknown"));
    }

    @Test
    void returnTypePredicates_TemporalTypesAreDistinct() {

        // LocalDateTime and OffsetDateTime are separate expression types in the grammar, and must not share a
        // classifier - otherwise a function of one would be looked up in the rule of the other
        QLFunctions functions = QLFunctions.builder()
                .function("asDate", new DateFunction())
                .function("asOffsetDateTime", new OffsetDateTimeFunction())
                .build();

        assertTrue(functions.dateFn("asDate"));
        assertFalse(functions.dateTimeFn("asDate"));
        assertFalse(functions.offsetDateTimeFn("asDate"));

        assertTrue(functions.offsetDateTimeFn("asOffsetDateTime"));
        assertFalse(functions.dateTimeFn("asOffsetDateTime"));
    }

    @Test
    void function_Matching2ArgTypes() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .function("sum", new Int3SumFunction())
                .build();

        QLFunctionDescriptor result = functions.function("sum", NUMERIC, List.of(arg(NUMERIC), arg(NUMERIC)));

        assertNotNull(result);
        assertEquals("sum", result.name());
        assertEquals(NUMERIC, result.returnType());
        assertEquals(2, result.args().length);
    }

    @Test
    void function_Matching3ArgTypes() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .function("sum", new Int3SumFunction())
                .build();

        QLFunctionDescriptor result = functions.function("sum", NUMERIC,
                List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));

        assertNotNull(result);
        assertEquals("sum", result.name());
        assertEquals(NUMERIC, result.returnType());
        assertEquals(3, result.args().length);
    }

    @Test
    void function_NoMatchingName() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .build();
        List<Arg> argTypes = List.of(arg(NUMERIC), arg(NUMERIC));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("multiply", NUMERIC, argTypes)
        );
        assertEquals("Function NUMERIC multiply([NUMERIC, NUMERIC]) not found", exception.getMessage());
    }

    @Test
    void function_WrongArgTypes() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .build();
        List<Arg> wrongArgTypes = List.of(arg(NUMERIC), arg(STRING));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("sum", NUMERIC, wrongArgTypes)
        );
        assertEquals("Function NUMERIC sum([NUMERIC, STRING]) not found", exception.getMessage());
    }

    @Test
    void function_WrongReturnType() {
        QLFunctions functions = QLFunctions.builder()
                .function("concat", (Udf2<String, String, String>) Exp::concat)
                .build();
        List<Arg> argTypes = List.of(arg(STRING), arg(STRING));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("concat", NUMERIC, argTypes)
        );
        assertEquals("Function NUMERIC concat([STRING, STRING]) not found", exception.getMessage());
    }

    @Test
    void function_ConstantArg_RenderedInNotFoundMessage() {
        QLFunctions functions = QLFunctions.builder()
                .function("substr", new StrConstIntFunction())
                .build();

        // a plain argument renders as the bare type name, a constant one is called out
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("substr", STRING, List.of(arg(STRING), constant(STRING)))
        );
        assertEquals("Function STRING substr([STRING, const STRING]) not found", exception.getMessage());
    }

    @Test
    void function_VarArgs_MatchesAnyArity() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new IntNSumFunction())
                .build();

        // match with 2 args
        QLFunctionDescriptor result2 = functions.function("sum", NUMERIC, List.of(arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result2);
        assertTrue(result2.isVarArgs());

        // match with 3 args
        QLFunctionDescriptor result3 = functions.function("sum", NUMERIC,
                List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result3);
        assertTrue(result3.isVarArgs());

        // match with 1 arg
        QLFunctionDescriptor result1 = functions.function("sum", NUMERIC, List.of(arg(NUMERIC)));
        assertNotNull(result1);
        assertTrue(result1.isVarArgs());

        // match with 0 args
        QLFunctionDescriptor result0 = functions.function("sum", NUMERIC, List.of());
        assertNotNull(result0);
        assertTrue(result0.isVarArgs());
    }

    @Test
    void function_VarArgs_FixedArityPreferred() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .function("sum", new IntNSumFunction())
                .build();

        // 2-arg call should prefer the fixed-arity Udf2
        QLFunctionDescriptor result2 = functions.function("sum", NUMERIC, List.of(arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result2);
        assertFalse(result2.isVarArgs());
        assertEquals(2, result2.args().length);

        // 3-arg call should fall back to varargs
        QLFunctionDescriptor result3 = functions.function("sum", NUMERIC,
                List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result3);
        assertTrue(result3.isVarArgs());
    }

    @Test
    void function_AnyArgType_MatchesTypedParam() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .build();

        // ANY is the classification of an argument whose type is only known at eval time. It must be accepted by a
        // parameter of any declared type
        QLFunctionDescriptor result = functions.function("sum", NUMERIC, List.of(arg(ANY), arg(ANY)));

        assertNotNull(result);
        assertEquals(2, result.args().length);
    }

    @Test
    void function_ObjectArgType_DoesNotMatchTypedParam() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .build();

        // unlike ANY, OBJECT is a deliberately Object-typed argument (a null literal, most notably), and must not
        // be silently passed to a numeric parameter
        List<Arg> argTypes = List.of(arg(NUMERIC), arg(OBJECT));

        assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("sum", NUMERIC, argTypes)
        );
    }

    @Test
    void function_ExactMatchPreferredOverWildcard() {

        // the wildcard overload is registered first, so specificity rather than registration order has to decide
        QLFunctions functions = QLFunctions.builder()
                .function("f", new ObjectArgFunction())
                .function("f", new StrArgFunction())
                .build();

        QLFunctionDescriptor result = functions.function("f", STRING, List.of(arg(STRING)));

        assertArrayEquals(new Arg[]{arg(STRING)}, result.args());
    }

    @Test
    void function_AnyArg_PrefersWildcardParam() {
        QLFunctions functions = QLFunctions.builder()
                .function("f", new StrArgFunction())
                .function("f", new ObjectArgFunction())
                .build();

        // an ANY argument matches both overloads, but only the one declaring OBJECT is written to handle an
        // argument of any type, so it is the more specific match
        QLFunctionDescriptor result = functions.function("f", STRING, List.of(arg(ANY)));

        assertArrayEquals(new Arg[]{arg(OBJECT)}, result.args());
    }

    @Test
    void function_EquallySpecific_FirstRegisteredWins() {

        // neither overload is more specific than the other: each matches one argument exactly and the other via a
        // wildcard. The tie must resolve to whichever was registered first
        List<Arg> argTypes = List.of(arg(STRING), arg(STRING));

        QLFunctions strFirst = QLFunctions.builder()
                .function("f", new StrObjArgFunction())
                .function("f", new ObjStrArgFunction())
                .build();

        assertArrayEquals(
                new Arg[]{arg(STRING), arg(OBJECT)},
                strFirst.function("f", STRING, argTypes).args());

        QLFunctions objFirst = QLFunctions.builder()
                .function("f", new ObjStrArgFunction())
                .function("f", new StrObjArgFunction())
                .build();

        assertArrayEquals(
                new Arg[]{arg(OBJECT), arg(STRING)},
                objFirst.function("f", STRING, argTypes).args());
    }

    @Test
    void function_ConstantParam_MatchesConstantArg() {
        QLFunctions functions = QLFunctions.builder()
                .function("substr", new StrConstIntFunction())
                .build();

        QLFunctionDescriptor result = functions.function("substr", STRING, List.of(arg(STRING), constant(NUMERIC)));

        assertNotNull(result);
        assertArrayEquals(new Arg[]{arg(OBJECT), constant(NUMERIC)}, result.args());
    }

    @Test
    void function_ConstantParam_RejectsNonConstantArg() {
        QLFunctions functions = QLFunctions.builder()
                .function("substr", new StrConstIntFunction())
                .build();

        // a column reference is a numeric expression, so it matches on type, but its value is not known until eval
        List<Arg> argTypes = List.of(arg(STRING), arg(NUMERIC));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("substr", STRING, argTypes)
        );
        assertEquals("Function STRING substr([STRING, NUMERIC]) not found", exception.getMessage());
    }

    @Test
    void function_ConstantParam_UntypedArgStillRejected() {
        QLFunctions functions = QLFunctions.builder()
                .function("substr", new StrConstIntFunction())
                .build();

        // ANY is compatible with any parameter type, but it is by definition not a constant
        assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("substr", STRING, List.of(arg(STRING), arg(ANY)))
        );
    }

    @Test
    void function_PlainParam_AcceptsConstantArg() {
        QLFunctions functions = QLFunctions.builder()
                .function("sum", new Int2SumFunction())
                .build();

        // passing a literal to an unconstrained parameter stays legal - this is what "abs(-5)" does
        QLFunctionDescriptor result = functions.function("sum", NUMERIC,
                List.of(constant(NUMERIC), constant(NUMERIC)));

        assertNotNull(result);
    }

    @Test
    void function_ConstancyDistinguishesOverloads() {

        // the same signature with a different constancy is a different function, and both must register
        QLFunctions functions = QLFunctions.builder()
                .function("substr", new StrConstIntFunction())
                .function("substr", new StrIntFunction())
                .build();

        assertArrayEquals(
                new Arg[]{arg(OBJECT), constant(NUMERIC)},
                functions.function("substr", STRING, List.of(arg(STRING), constant(NUMERIC))).args());

        assertArrayEquals(
                new Arg[]{arg(OBJECT), arg(NUMERIC)},
                functions.function("substr", STRING, List.of(arg(STRING), arg(NUMERIC))).args());
    }

    @Test
    void function_SameFunctionIsADuplicate() {
        QLFunctions.Builder builder = QLFunctions.builder().function("substr", new StrConstIntFunction());

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
                () -> QLFunctions.builder().function("sum", new ConstVarArgsFunction())
        );
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
