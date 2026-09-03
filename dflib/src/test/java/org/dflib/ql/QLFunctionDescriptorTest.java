package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DecimalExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.UdfN;
import org.dflib.exp.flow.IfExp;
import org.dflib.exp.flow.IfNullExp;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.dflib.Exp.$bool;
import static org.dflib.Exp.$col;
import static org.dflib.Exp.$date;
import static org.dflib.Exp.$dateTime;
import static org.dflib.Exp.$int;
import static org.dflib.Exp.$offsetDateTime;
import static org.dflib.Exp.$str;
import static org.dflib.Exp.$time;
import static org.junit.jupiter.api.Assertions.*;

class QLFunctionDescriptorTest {

    @Test
    void test() {
        QLFunctionDescriptor descriptor = new QLFunctionDescriptor("add", QLFunctionSignature.udf1(new AdditionFn()));
        assertNotNull(descriptor);

        Exp<?> fnCall = descriptor.expProducer().apply(List.of($int("a")));
        assertNotNull(fnCall);
    }

    @Test
    void udfN() {
        QLFunctionDescriptor descriptor = new QLFunctionDescriptor("vadd", QLFunctionSignature.udfN(new VarArgsFn()));
        assertNotNull(descriptor);
        assertTrue(descriptor.isVarArgs());
        assertEquals(0, descriptor.args().length);
        assertEquals(QLFunctionDescriptor.TypeClassifier.NUMERIC, descriptor.returnType());

        Exp<?> fnCall = descriptor.expProducer().apply(List.of($int("a"), $int("b")));
        assertNotNull(fnCall);
    }

    @Test
    void returnType_NonParameterizedExpInterface() {

        // DecimalExp and Condition take no type parameters of their own, so the value type has to be recovered from
        // the "Exp<X>" they extend. Otherwise such a function classifies as OBJECT and is never found
        assertEquals(
                QLFunctionDescriptor.TypeClassifier.NUMERIC,
                new QLFunctionDescriptor("dec", QLFunctionSignature.udf1(new DecimalFn())).returnType());

        assertEquals(
                QLFunctionDescriptor.TypeClassifier.BOOLEAN,
                new QLFunctionDescriptor("cond", QLFunctionSignature.udf1(new ConditionFn())).returnType());
    }

    @Test
    void returnType_NestedGenerics() {

        // only the expression layer is unwrapped: the value type of "Exp<List<String>>" is List, not String
        assertEquals(
                QLFunctionDescriptor.TypeClassifier.OBJECT,
                new QLFunctionDescriptor("list", QLFunctionSignature.udf1(new ListFn())).returnType());
    }

    @Test
    void returnType_ArrayValueType() {

        // "split(..)" and friends produce an array-valued expression. String[] is not a CharSequence, so it must
        // classify as OBJECT rather than leaking into the STRING namespace
        assertEquals(
                QLFunctionDescriptor.TypeClassifier.OBJECT,
                new QLFunctionDescriptor("split", QLFunctionSignature.udf1(new ArrayFn())).returnType());
    }

    @Test
    void constantArg_CovariantReturn() {

        // a covariant return makes javac emit a bridge "call" with the same erased parameters. The bridge carries
        // neither the generic types nor the parameter annotations, so picking it would silently drop @Constant
        QLFunctionDescriptor descriptor = new QLFunctionDescriptor("scale", QLFunctionSignature.udf2(new ConstArgNumFn()));

        assertEquals(QLFunctionDescriptor.TypeClassifier.NUMERIC, descriptor.returnType());
        assertArrayEquals(
                new QLFunctionDescriptor.Arg[]{
                        new QLFunctionDescriptor.Arg(QLFunctionDescriptor.TypeClassifier.NUMERIC, false),
                        new QLFunctionDescriptor.Arg(QLFunctionDescriptor.TypeClassifier.NUMERIC, true)},
                descriptor.args());
    }

    @Test
    void classifyExp_TypedInterfaces() {
        assertEquals(TypeClassifier.NUMERIC, TypeClassifier.classify($int("a")));
        assertEquals(TypeClassifier.STRING, TypeClassifier.classify($str("a")));
        assertEquals(TypeClassifier.BOOLEAN, TypeClassifier.classify($bool("a")));
        assertEquals(TypeClassifier.DATE, TypeClassifier.classify($date("a")));
        assertEquals(TypeClassifier.TIME, TypeClassifier.classify($time("a")));
        assertEquals(TypeClassifier.DATETIME, TypeClassifier.classify($dateTime("a")));
        assertEquals(TypeClassifier.OFFSETDATETIME, TypeClassifier.classify($offsetDateTime("a")));
    }

    @Test
    void classifyExp_TypedExpWithoutATypedInterface() {

        // "first(date(a))" is a FirstExp<LocalDate>, not a DateExp; "if(c, int(a), int(b))" is an IfExp<Integer>,
        // not a NumExp. The value type is there, but it can only be recovered at eval time. Classifying such an
        // expression as OBJECT would make "year(first(date(a)))" unresolvable
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($date("a").first()));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($int("a").last()));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify(new IfNullExp<>($int("a"), $int("b"))));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify(new IfExp<>($bool("c"), $int("a"), $int("b"))));
    }

    @Test
    void classifyExp_UntypedExp() {

        // an expression with no static type at all is ANY, as it always was
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($col("a")));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($col("a").first()));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify(new IfNullExp<>($col("a"), $col("b"))));
    }

    @Test
    void classifyExp_ObjectValuedExp() {

        // an expression whose value type is a genuine Object stays OBJECT: unlike ANY it must not be silently
        // passed to a typed parameter
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify($str("a").split(',')));
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify($col("a").list()));
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify($col("a").set()));

        // ... and so does a scalar of an unrecognized type
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify(Exp.$val(new Object())));
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify(Exp.$val(null)));
    }

    @Test
    void classifyExp_Null() {
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify((Exp<?>) null));
    }

    @Test
    void isTyped() {
        for (TypeClassifier t : TypeClassifier.values()) {
            assertEquals(t != TypeClassifier.OBJECT && t != TypeClassifier.ANY, t.isTyped(), t.name());
            assertEquals(t.isTyped(), t.castFunction() != null, t.name());
        }
    }

    @Test
    void returnType_Fixed() {
        QLFunctionDescriptor descriptor = new QLFunctionDescriptor("len", QLFunctionSignature.signature()
                .returning(TypeClassifier.NUMERIC)
                .arg(TypeClassifier.STRING)
                .as(args -> args.get(0).castAsStr().len()));

        assertEquals(TypeClassifier.NUMERIC, descriptor.returnType());
    }

    @Test
    void equals_IgnoresReturnType() {

        // resolution is driven by name + args, so two descriptors with the same argument shape are unresolvable
        // whatever they return. Equality reflects that, turning such a pair into a build-time error
        QLFunctionDescriptor str = new QLFunctionDescriptor("f", QLFunctionSignature.signature()
                .returning(TypeClassifier.STRING)
                .arg(TypeClassifier.NUMERIC)
                .as(args -> args.get(0).castAsStr()));

        QLFunctionDescriptor bool = new QLFunctionDescriptor("f", QLFunctionSignature.signature()
                .returning(TypeClassifier.BOOLEAN)
                .arg(TypeClassifier.NUMERIC)
                .as(args -> args.get(0).castAsBool()));

        assertEquals(str, bool);
        assertEquals(str.hashCode(), bool.hashCode());
    }

    @Test
    void reflect_ProducesTheSameDescriptorAsAnExplicitSignature() {

        // the reflective Udf path is implemented on top of the explicit one
        QLFunctionDescriptor reflected = new QLFunctionDescriptor("scale", QLFunctionSignature.udf2(new ConstArgNumFn()));

        QLFunctionDescriptor explicit = new QLFunctionDescriptor("scale", QLFunctionSignature.signature()
                .returning(TypeClassifier.NUMERIC)
                .arg(TypeClassifier.NUMERIC)
                .constArg(TypeClassifier.NUMERIC)
                .as(args -> args.get(0).castAsDecimal()));

        assertEquals(explicit, reflected);
        assertEquals(explicit.returnType(), reflected.returnType());
    }

    public static class ConstArgNumFn implements Udf2<Number, Integer, Number> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public NumExp<Number> call(Exp<Number> exp, @Constant Exp<Integer> scale) {
            return (NumExp) exp.castAsDecimal();
        }
    }

    public static class DecimalFn implements Udf1<Number, BigDecimal> {
        @Override
        public DecimalExp call(Exp<Number> exp) {
            return exp.castAsDecimal();
        }
    }

    public static class ConditionFn implements Udf1<Object, Boolean> {
        @Override
        public Condition call(Exp<Object> exp) {
            return exp.castAsBool();
        }
    }

    public static class ArrayFn implements Udf1<String, String[]> {
        @Override
        public Exp<String[]> call(Exp<String> exp) {
            return exp.castAsStr().split(',');
        }
    }

    public static class ListFn implements Udf1<Object, List<String>> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Exp<List<String>> call(Exp<Object> exp) {
            return (Exp) exp.castAsStr().list();
        }
    }

    public static class VarArgsFn implements UdfN<Number> {
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

    public static class AdditionFn implements Udf1<Number, Number> {

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public NumExp<Number> call(Exp<Number> exp) {
            return ((NumExp) exp).add(1);
        }
    }
}
