package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.DecimalExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link TypeClassifier#classify(Type)}, the static classifier that maps a declared Java type - a method
 * return type or a parameter type - to a QL type classifier.
 */
class TypeClassifierTest {

    private static Method method(String name) {
        for (Method m : Fixtures.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new IllegalArgumentException("No such fixture method: " + name);
    }

    private static TypeClassifier returnOf(String method) {
        return TypeClassifier.classify(method(method).getGenericReturnType());
    }

    private static TypeClassifier paramOf(String method, int i) {
        return TypeClassifier.classify(method(method).getGenericParameterTypes()[i]);
    }

    @Test
    void numExp_Wildcard() {

        // the reflected upper bound of the "?" in "NumExp<?>" is Object: the interface-declared "N extends Number"
        // is not propagated into it. The interface itself is what makes the type numeric
        assertEquals(TypeClassifier.NUMERIC, paramOf("numWildcard", 0));
        assertEquals(TypeClassifier.NUMERIC, returnOf("numWildcard"));
    }

    @Test
    void numExp_Raw() {

        // a raw "NumExp" carries no type argument at all
        assertEquals(TypeClassifier.NUMERIC, paramOf("rawNum", 0));
        assertEquals(TypeClassifier.NUMERIC, returnOf("rawNum"));
    }

    @Test
    void numExp_TypeVariable() {

        // "<N extends Number> NumExp<N>": the argument is a type variable that reflection can not resolve to a
        // call site, but the interface already answers the question
        assertEquals(TypeClassifier.NUMERIC, paramOf("numVar", 0));
        assertEquals(TypeClassifier.NUMERIC, returnOf("numVar"));
    }

    @Test
    void numExp_Parameterized() {
        assertEquals(TypeClassifier.NUMERIC, paramOf("numParameterized", 0));
        assertEquals(TypeClassifier.NUMERIC, returnOf("numParameterized"));
    }

    @Test
    void decimalExp() {

        // DecimalExp is a NumExp and takes no type parameter of its own
        assertEquals(TypeClassifier.NUMERIC, paramOf("decimal", 0));
        assertEquals(TypeClassifier.NUMERIC, returnOf("decimal"));
    }

    @Test
    void typedExpInterfaces() {
        assertEquals(TypeClassifier.STRING, paramOf("str", 0));
        assertEquals(TypeClassifier.STRING, returnOf("str"));

        assertEquals(TypeClassifier.BOOLEAN, paramOf("cond", 0));
        assertEquals(TypeClassifier.BOOLEAN, returnOf("cond"));

        assertEquals(TypeClassifier.DATE, paramOf("date", 0));
        assertEquals(TypeClassifier.DATE, returnOf("date"));

        assertEquals(TypeClassifier.TIME, paramOf("time", 0));
        assertEquals(TypeClassifier.TIME, returnOf("time"));

        assertEquals(TypeClassifier.DATETIME, paramOf("dateTime", 0));
        assertEquals(TypeClassifier.DATETIME, returnOf("dateTime"));

        assertEquals(TypeClassifier.OFFSETDATETIME, paramOf("offsetDateTime", 0));
        assertEquals(TypeClassifier.OFFSETDATETIME, returnOf("offsetDateTime"));
    }

    @Test
    void bareTypeVariable() {

        // a non-Exp parameter is an implicit constant argument, so a bare "<N extends Number> N filler" must
        // classify by its bound. An unbounded "T" is worth Object
        assertEquals(TypeClassifier.NUMERIC, paramOf("bareNumVar", 0));
        assertEquals(TypeClassifier.NUMERIC, returnOf("bareNumVar"));

        assertEquals(TypeClassifier.OBJECT, paramOf("bareVar", 0));
        assertEquals(TypeClassifier.OBJECT, returnOf("bareVar"));
    }

    @Test
    void expOfATypeVariable() {

        // "<T> Exp<T>" is the shape of "first", "if" and "shift" over an untyped receiver: no static type
        assertEquals(TypeClassifier.OBJECT, paramOf("expVar", 0));
        assertEquals(TypeClassifier.OBJECT, returnOf("expVar"));
    }

    @Test
    void primitives() {

        // "Number.class.isAssignableFrom(int.class)" is false, so primitives have to be boxed before classifying
        assertEquals(TypeClassifier.NUMERIC, paramOf("primitives", 0)); // int
        assertEquals(TypeClassifier.NUMERIC, paramOf("primitives", 1)); // double
        assertEquals(TypeClassifier.NUMERIC, paramOf("primitives", 2)); // long
        assertEquals(TypeClassifier.NUMERIC, paramOf("primitives", 3)); // float
        assertEquals(TypeClassifier.NUMERIC, paramOf("primitives", 4)); // short
        assertEquals(TypeClassifier.NUMERIC, paramOf("primitives", 5)); // byte
        assertEquals(TypeClassifier.BOOLEAN, paramOf("primitives", 6)); // boolean

        // char is not a CharSequence and is not used as a QL argument type
        assertEquals(TypeClassifier.OBJECT, paramOf("primitives", 7)); // char

        assertEquals(TypeClassifier.NUMERIC, returnOf("primitives"));
    }

    @Test
    void valueTypes() {

        // implicit constant arguments declared by their value type rather than as an expression
        assertEquals(TypeClassifier.STRING, paramOf("values", 0));
        assertEquals(TypeClassifier.DATE, paramOf("values", 1));
        assertEquals(TypeClassifier.TIME, paramOf("values", 2));
        assertEquals(TypeClassifier.DATETIME, paramOf("values", 3));
        assertEquals(TypeClassifier.OFFSETDATETIME, paramOf("values", 4));
        assertEquals(TypeClassifier.NUMERIC, paramOf("values", 5)); // Integer
        assertEquals(TypeClassifier.BOOLEAN, paramOf("values", 6)); // Boolean
        assertEquals(TypeClassifier.OBJECT, paramOf("values", 7)); // Object
    }

    @Test
    void rawExp() {

        // a bare "Exp<V>" is not a typed interface: the value type is what classifies it
        assertEquals(TypeClassifier.OBJECT, paramOf("wildcardExp", 0));
        assertEquals(TypeClassifier.OBJECT, returnOf("wildcardExp"));

        assertEquals(TypeClassifier.NUMERIC, paramOf("intExp", 0));
        assertEquals(TypeClassifier.STRING, paramOf("strValueExp", 0));
        assertEquals(TypeClassifier.OBJECT, paramOf("objectExp", 0));
    }

    @Test
    void expOfAnArray() {

        // "split()" produces an array-valued expression. String[] is not a CharSequence, so it must not leak into
        // the STRING namespace
        assertEquals(TypeClassifier.OBJECT, paramOf("arrayExp", 0));
        assertEquals(TypeClassifier.OBJECT, returnOf("arrayExp"));
    }

    @Test
    void expOfANestedGeneric() {

        // only the expression layer is unwrapped: the value type of "Exp<List<String>>" is List, not String
        assertEquals(TypeClassifier.OBJECT, paramOf("listExp", 0));
        assertEquals(TypeClassifier.OBJECT, returnOf("listExp"));
    }

    @Test
    void varArgs() {

        // "Exp<?>..." reflects as a generic array type. It is not a typed expression interface and classifies as
        // OBJECT, as it always did (vararg parameters are not turned into declared args at all - see
        // QLFunctionSignature.reflect)
        Method m = method("varargs");
        assertTrue(m.isVarArgs());
        assertEquals(TypeClassifier.OBJECT, paramOf("varargs", 0));
        assertEquals(TypeClassifier.NUMERIC, returnOf("varargs"));
    }

    @SuppressWarnings({"rawtypes", "unused"})
    static class Fixtures {

        NumExp<?> numWildcard(NumExp<?> e) {
            return e;
        }

        NumExp rawNum(NumExp e) {
            return e;
        }

        <N extends Number> NumExp<N> numVar(NumExp<N> e) {
            return e;
        }

        NumExp<Integer> numParameterized(NumExp<Integer> e) {
            return e;
        }

        DecimalExp decimal(DecimalExp e) {
            return e;
        }

        StrExp str(StrExp e) {
            return e;
        }

        Condition cond(Condition e) {
            return e;
        }

        DateExp date(DateExp e) {
            return e;
        }

        TimeExp time(TimeExp e) {
            return e;
        }

        DateTimeExp dateTime(DateTimeExp e) {
            return e;
        }

        OffsetDateTimeExp offsetDateTime(OffsetDateTimeExp e) {
            return e;
        }

        <N extends Number> N bareNumVar(N filler) {
            return filler;
        }

        <T> T bareVar(T v) {
            return v;
        }

        <T> Exp<T> expVar(Exp<T> e) {
            return e;
        }

        int primitives(int i, double d, long l, float f, short s, byte b, boolean bool, char c) {
            return i;
        }

        void values(String s, LocalDate d, LocalTime t, LocalDateTime dt, OffsetDateTime odt, Integer i,
                    Boolean b, Object o) {
        }

        Exp<?> wildcardExp(Exp<?> e) {
            return e;
        }

        Exp<Integer> intExp(Exp<Integer> e) {
            return e;
        }

        Exp<String> strValueExp(Exp<String> e) {
            return e;
        }

        Exp<Object> objectExp(Exp<Object> e) {
            return e;
        }

        Exp<String[]> arrayExp(Exp<String[]> e) {
            return e;
        }

        Exp<List<String>> listExp(Exp<List<String>> e) {
            return e;
        }

        NumExp<?> varargs(Exp<?>... exps) {
            return (NumExp<?>) exps[0];
        }
    }
}
