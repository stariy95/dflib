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
import static org.dflib.ql.DescriptorBuilder.descriptor;
import static org.junit.jupiter.api.Assertions.*;

class QLFunctionDescriptorTest {

    @Test
    void test() {
        QLFunctionDescriptor descriptor = QLFunctionReflection.udf1("add", new AdditionFn());
        assertNotNull(descriptor);

        Exp<?> fnCall = descriptor.expProducer().apply(List.of($int("a")));
        assertNotNull(fnCall);
    }

    @Test
    void udfN() {
        QLFunctionDescriptor descriptor = QLFunctionReflection.udfN("vadd", new VarArgsFn());
        assertNotNull(descriptor);
        assertTrue(descriptor.varArgs());
        assertEquals(0, descriptor.args().size());
        assertEquals(TypeClassifier.NUMERIC, descriptor.returnType());

        Exp<?> fnCall = descriptor.expProducer().apply(List.of($int("a"), $int("b")));
        assertNotNull(fnCall);
    }

    @Test
    void returnType_NonParameterizedExpInterface() {
        assertEquals(
                TypeClassifier.NUMERIC,
                QLFunctionReflection.udf1("dec", new DecimalFn()).returnType());

        assertEquals(
                TypeClassifier.BOOLEAN,
                QLFunctionReflection.udf1("cond", new ConditionFn()).returnType());
    }

    @Test
    void returnType_NestedGenerics() {
        assertEquals(
                TypeClassifier.OBJECT,
                QLFunctionReflection.udf1("list", new ListFn()).returnType());
    }

    @Test
    void returnType_ArrayValueType() {
        assertEquals(
                TypeClassifier.OBJECT,
                QLFunctionReflection.udf1("split", new ArrayFn()).returnType());
    }

    @Test
    void covariantReturn() {

        // the bridge "call" emitted for a covariant return carries no generic types
        QLFunctionDescriptor descriptor = QLFunctionReflection.udf2("scale", new CovariantFn());

        assertEquals(TypeClassifier.NUMERIC, descriptor.returnType());
        assertEquals(
                List.of(
                        new QLFunctionArg(TypeClassifier.NUMERIC, false),
                        new QLFunctionArg(TypeClassifier.NUMERIC, false)),
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
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($date("a").first()));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($int("a").last()));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify(new IfNullExp<>($int("a"), $int("b"))));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify(new IfExp<>($bool("c"), $int("a"), $int("b"))));
    }

    @Test
    void classifyExp_UntypedExp() {
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($col("a")));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify($col("a").first()));
        assertEquals(TypeClassifier.ANY, TypeClassifier.classify(new IfNullExp<>($col("a"), $col("b"))));
    }

    @Test
    void classifyExp_ObjectValuedExp() {
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify($str("a").split(',')));
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify($col("a").list()));
        assertEquals(TypeClassifier.OBJECT, TypeClassifier.classify($col("a").set()));
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
        QLFunctionDescriptor descriptor = descriptor("len")
                .returning(TypeClassifier.NUMERIC)
                .arg(TypeClassifier.STRING)
                .as(args -> args.get(0).castAsStr().len());

        assertEquals(TypeClassifier.NUMERIC, descriptor.returnType());
    }

    @Test
    void equals_IgnoresReturnType() {
        QLFunctionDescriptor str = descriptor("f")
                .returning(TypeClassifier.STRING)
                .arg(TypeClassifier.NUMERIC)
                .as(args -> args.get(0).castAsStr());

        QLFunctionDescriptor bool = descriptor("f")
                .returning(TypeClassifier.BOOLEAN)
                .arg(TypeClassifier.NUMERIC)
                .as(args -> args.get(0).castAsBool());

        assertEquals(str, bool);
        assertEquals(str.hashCode(), bool.hashCode());
    }

    @Test
    void reflect_ProducesTheSameDescriptorAsAnExplicitSignature() {
        QLFunctionDescriptor reflected = QLFunctionReflection.udf2("scale", new CovariantFn());

        QLFunctionDescriptor explicit = descriptor("scale")
                .returning(TypeClassifier.NUMERIC)
                .arg(TypeClassifier.NUMERIC)
                .arg(TypeClassifier.NUMERIC)
                .as(args -> args.get(0).castAsDecimal());

        assertEquals(explicit, reflected);
        assertEquals(explicit.returnType(), reflected.returnType());
    }

    public static class CovariantFn implements Udf2<Number, Integer, Number> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public NumExp<Number> call(Exp<Number> exp, Exp<Integer> scale) {
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
