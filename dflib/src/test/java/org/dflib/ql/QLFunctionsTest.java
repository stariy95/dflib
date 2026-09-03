package org.dflib.ql;

import org.dflib.*;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.dflib.ql.DescriptorBuilder.descriptor;
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

        assertTrue(functions.mayReturn("split", OBJECT));
        assertFalse(functions.mayReturn("split", STRING));

        for (TypeClassifier t : TypeClassifier.values()) {
            assertFalse(functions.mayReturn("unknown", t), t.name());
        }
    }

    @Test
    void mayReturn_TemporalTypesAreDistinct() {
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

        QLFunctionDescriptor result2 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result2);
        assertTrue(result2.isVarArgs());

        QLFunctionDescriptor result3 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result3);
        assertTrue(result3.isVarArgs());

        QLFunctionDescriptor result1 = functions.function("sum", List.of(arg(NUMERIC)));
        assertNotNull(result1);
        assertTrue(result1.isVarArgs());

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

        QLFunctionDescriptor result2 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result2);
        assertFalse(result2.isVarArgs());
        assertEquals(2, result2.args().length);

        QLFunctionDescriptor result3 = functions.function("sum", List.of(arg(NUMERIC), arg(NUMERIC), arg(NUMERIC)));
        assertNotNull(result3);
        assertTrue(result3.isVarArgs());
    }

    @Test
    void function_AnyArgType_MatchesTypedParam() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .build();

        QLFunctionDescriptor result = functions.function("sum", List.of(arg(ANY), arg(ANY)));

        assertNotNull(result);
        assertEquals(2, result.args().length);
    }

    @Test
    void function_ObjectArgType_DoesNotMatchTypedParam() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .build();

        List<Arg> argTypes = List.of(arg(NUMERIC), arg(OBJECT));

        assertThrows(
                IllegalArgumentException.class,
                () -> functions.function("sum", argTypes)
        );
    }

    @Test
    void function_ExactMatchPreferredOverWildcard() {
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

        QLFunctionDescriptor result = functions.function("f", List.of(arg(ANY)));

        assertArrayEquals(new Arg[]{arg(OBJECT)}, result.args());
    }

    @Test
    void function_EquallySpecific_FirstRegisteredWins() {
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

    private static QLFunctions.Builder twoReceivers(TypeClassifier a, TypeClassifier b) {
        return QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f").returning(STRING).arg(a).as(args -> args.get(0).castAsStr()))
                .function(descriptor("f").returning(STRING).arg(b).as(args -> args.get(0).castAsStr()));
    }

    @Test
    void function_AnyArg_AmbiguousAmongTypedOverloads() {
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
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f")
                        .returning(STRING).arg(BOOLEAN).arg(DATE).as(args -> args.get(0).castAsStr()))
                .function(descriptor("f")
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
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f")
                        .returning(STRING).arg(NUMERIC).arg(BOOLEAN).as(args -> args.get(0).castAsStr()))
                .function(descriptor("f")
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
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f")
                        .returning(STRING).arg(DATE).arg(STRING).arg(OBJECT).as(args -> args.get(0).castAsStr()))
                .function(descriptor("f")
                        .returning(STRING).arg(DATE).arg(OBJECT).arg(STRING).as(args -> args.get(0).castAsStr()))
                .build();

        assertArrayEquals(
                new Arg[]{arg(DATE), arg(STRING), arg(OBJECT)},
                functions.function("f", List.of(arg(ANY), arg(STRING), arg(STRING))).args());
    }

    @Test
    void function_AnyArg_WildcardOverloadResolvesTheAmbiguity() {
        QLFunctions functions = twoReceivers(NUMERIC, DATE)
                .function(descriptor("f")
                        .returning(STRING).arg(OBJECT).as(args -> args.get(0).castAsStr()))
                .build();

        assertArrayEquals(new Arg[]{arg(OBJECT)}, functions.function("f", List.of(arg(ANY))).args());
    }

    @Test
    void function_AnyArg_WildcardOverloadWinsATie() {

        // "shift(a, 1, 'x')": an ANY argument to a typed parameter costs more than any number of wildcard matches
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f")
                        .returning(STRING).arg(STRING).arg(STRING).as(args -> args.get(0).castAsStr()))
                .function(descriptor("f")
                        .returning(OBJECT).arg(OBJECT).arg(OBJECT).as(args -> args.get(0).castAsStr()))
                .build();

        assertArrayEquals(
                new Arg[]{arg(OBJECT), arg(OBJECT)},
                functions.function("f", List.of(arg(ANY), arg(STRING))).args());

        assertArrayEquals(
                new Arg[]{arg(STRING), arg(STRING)},
                functions.function("f", List.of(arg(STRING), arg(STRING))).args());
    }

    @Test
    void function_AnyArg_FixedArityWinsOverVarArgs() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f")
                        .returning(STRING).arg(NUMERIC).as(args -> args.get(0).castAsStr()))
                .function(descriptor("f")
                        .returning(STRING).varArgs().as(args -> args.get(0).castAsStr()))
                .build();

        QLFunctionDescriptor descriptor = functions.function("f", List.of(arg(ANY)));

        assertFalse(descriptor.isVarArgs());
        assertArrayEquals(new Arg[]{arg(NUMERIC)}, descriptor.args());
    }

    @Test
    void function_AnyArg_VarArgsAmbiguousOnALeadingParam() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f")
                        .returning(STRING).arg(NUMERIC).varArgs().as(args -> args.get(0).castAsStr()))
                .function(descriptor("f")
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

        QLFunctionDescriptor result = functions.function("sum", List.of(constant(NUMERIC), constant(NUMERIC)));

        assertNotNull(result);
    }

    @Test
    void function_ConstancyDistinguishesOverloads() {
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
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function("f", new ObjectArgFunction())
                .function("f", new Int2SumFunction())
                .build();

        assertTrue(functions.mayReturn("f", STRING));
        assertTrue(functions.mayReturn("f", NUMERIC));
        assertFalse(functions.mayReturn("f", BOOLEAN));
    }

    @Test
    void mayReturn_OverloadPerReceiver() {
        QLFunctions functions = IdentityFunctions.identity(
                QLFunctions.builder().noDefaultFunctions(), "shift", constant(NUMERIC)).build();

        for (TypeClassifier t : TypeClassifier.values()) {
            assertEquals(t != OBJECT, functions.mayReturn("shift", t), t.name());
        }
    }

    @Test
    void mayReturn_AnyReturningFunction() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("first")
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
        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions()
                .function("sum", new Int2SumFunction())
                .function(descriptor("split")
                        .returning(OBJECT)
                        .arg(OBJECT)
                        .as(args -> args.get(0).list()))
                .function(descriptor("first")
                        .returning(ANY)
                        .arg(OBJECT)
                        .as(args -> args.get(0).first()));
        QLFunctions functions = IdentityFunctions.identity(builder, "shift", constant(NUMERIC)).build();

        assertTrue(functions.hasTypedReturn("sum"));
        assertTrue(functions.hasTypedReturn("shift"));
        assertFalse(functions.hasTypedReturn("split"));
        assertFalse(functions.hasTypedReturn("first"));

        assertFalse(functions.hasTypedReturn("unknown"));
    }

    @Test
    void isPolymorphicFn() {
        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions()
                .function("trim", new ObjectArgFunction())
                .function("sum", new Int2SumFunction())
                .function("sum", new Int3SumFunction())
                .function("f", new ObjectArgFunction())
                .function("f", new Int2SumFunction());
        QLFunctions functions = IdentityFunctions.identity(builder, "shift").build();

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
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("year")
                        .returning(NUMERIC)
                        .arg(DATE)
                        .as(args -> ((DateExp) args.get(0)).year()))
                .build();

        QLFunctionDescriptor descriptor = functions.function("year", List.of(Arg.of(Exp.$date("a").first())));

        assertNotNull(descriptor);
        assertEquals(NUMERIC, descriptor.returnType());
    }

    @Test
    void signature_FourArgs() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("between")
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
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("concat")
                        .returning(STRING)
                        .varArgs()
                        .as(args -> args.get(0).castAsStr()))
                .function(descriptor("concat")
                        .returning(STRING)
                        .as(args -> Exp.$strVal("")))
                .build();

        assertFalse(functions.function("concat", List.of()).isVarArgs());
        assertTrue(functions.function("concat", List.of(arg(STRING))).isVarArgs());
        assertTrue(functions.function("concat", List.of(arg(STRING), arg(NUMERIC))).isVarArgs());
    }

    @Test
    void signature_VarArgsWithLeadingTypedParams() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("vConcat")
                        .returning(STRING)
                        .constArg(STRING)
                        .varArgs()
                        .as(args -> args.get(1).castAsStr()))
                .build();

        assertNotNull(functions.function("vConcat", List.of(constant(STRING), arg(NUMERIC), arg(DATE))));
        assertNotNull(functions.function("vConcat", List.of(constant(STRING))));

        assertThrows(IllegalArgumentException.class, () -> functions.function("vConcat", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> functions.function("vConcat", List.of(arg(STRING), arg(STRING))));
        assertThrows(IllegalArgumentException.class,
                () -> functions.function("vConcat", List.of(constant(NUMERIC), arg(STRING))));
    }

    @Test
    void signature_ConstArgNeedsNoAnnotation() {
        QLFunctions functions = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("substr")
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
    void signature_DuplicateShapeRejected() {
        QLFunctions.Builder builder = QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f")
                        .returning(STRING)
                        .arg(NUMERIC)
                        .as(args -> args.get(0).castAsStr()));

        assertThrows(IllegalArgumentException.class, () -> builder
                .function(descriptor("f")
                        .returning(BOOLEAN)
                        .arg(NUMERIC)
                        .as(args -> args.get(0).castAsBool())));
    }

    @Test
    void signature_ProducerRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> descriptor("f").returning(STRING).arg(NUMERIC).as(null));
    }

    @Test
    void signature_ReturnTypeRequired() {
        assertThrows(IllegalArgumentException.class, () -> QLFunctions.builder().noDefaultFunctions()
                .function(descriptor("f").arg(NUMERIC).as(args -> args.get(0))));
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

    public static class StrConstIntFunction implements QLFunction {
        public StrExp call(Exp<?> exp, int from) {
            return exp.substr(from);
        }
    }

    private static class StrIntFunction implements Udf2<Object, Integer, String> {
        @Override
        public Exp<String> call(Exp<Object> exp, Exp<Integer> from) {
            return exp.castAsStr();
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
