package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.exp.fn.Constant;
import org.dflib.DecimalExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.UdfN;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.dflib.Exp.$int;
import static org.junit.jupiter.api.Assertions.*;

class QLFunctionDescriptorTest {


    @Test
    void test() {
        QLFunctionDescriptor descriptor = QLFunctionDescriptor.ofUdf1(new AdditionFn()).name("add").build();
        assertNotNull(descriptor);

        Exp<?> fnCall = descriptor.expProducer().apply(List.of($int("a")));
        assertNotNull(fnCall);
    }

    @Test
    void udfN() {
        QLFunctionDescriptor descriptor = QLFunctionDescriptor.ofUdfN(new VarArgsFn()).name("vadd").build();
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
                QLFunctionDescriptor.ofUdf1(new DecimalFn()).name("dec").build().returnType());

        assertEquals(
                QLFunctionDescriptor.TypeClassifier.BOOLEAN,
                QLFunctionDescriptor.ofUdf1(new ConditionFn()).name("cond").build().returnType());
    }

    @Test
    void returnType_NestedGenerics() {

        // only the expression layer is unwrapped: the value type of "Exp<List<String>>" is List, not String
        assertEquals(
                QLFunctionDescriptor.TypeClassifier.OBJECT,
                QLFunctionDescriptor.ofUdf1(new ListFn()).name("list").build().returnType());
    }

    @Test
    void constantArg_CovariantReturn() {

        // a covariant return makes javac emit a bridge "call" with the same erased parameters. The bridge carries
        // neither the generic types nor the parameter annotations, so picking it would silently drop @Constant
        QLFunctionDescriptor descriptor = QLFunctionDescriptor.ofUdf2(new ConstArgNumFn()).name("scale").build();

        assertEquals(QLFunctionDescriptor.TypeClassifier.NUMERIC, descriptor.returnType());
        assertArrayEquals(
                new QLFunctionDescriptor.Arg[]{
                        new QLFunctionDescriptor.Arg(QLFunctionDescriptor.TypeClassifier.NUMERIC, false),
                        new QLFunctionDescriptor.Arg(QLFunctionDescriptor.TypeClassifier.NUMERIC, true)},
                descriptor.args());
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