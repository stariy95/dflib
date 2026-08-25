package org.dflib.ql;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;
import org.dflib.exp.ScalarExp;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class QLFunctionDescriptor {

    final String name;
    final TypeClassifier returnType;
    final TypeClassifier[] argTypes;
    final boolean varArgs;
    final Function<List<Exp<?>>, Exp<?>> fnExpProducer;

    private QLFunctionDescriptor(String name,
                                 TypeClassifier returnType,
                                 TypeClassifier[] argTypes,
                                 boolean varArgs,
                                 Function<List<Exp<?>>, Exp<?>> fnExpProducer) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = argTypes;
        this.varArgs = varArgs;
        this.fnExpProducer = fnExpProducer;
    }

    public static Builder ofUdf0(Udf0<?> function) {
        return new Builder().udf0(function);
    }

    public static Builder ofUdf1(Udf1<?, ?> function) {
        return new Builder().udf1(function);
    }

    public static Builder ofUdf2(Udf2<?, ?, ?> function) {
        return new Builder().udf2(function);
    }

    public static Builder ofUdf3(Udf3<?, ?, ?, ?> function) {
        return new Builder().udf3(function);
    }

    public static Builder ofUdfN(UdfN<?> function) {
        return new Builder().udfN(function);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        QLFunctionDescriptor that = (QLFunctionDescriptor) o;
        return name.equals(that.name)
                && varArgs == that.varArgs
                && Arrays.equals(argTypes, that.argTypes);
    }

    public String name() {
        return name;
    }

    public TypeClassifier returnType() {
        return returnType;
    }

    public TypeClassifier[] argTypes() {
        return argTypes;
    }

    public boolean isVarArgs() {
        return varArgs;
    }

    public Function<List<Exp<?>>, Exp<?>> expProducer() {
        return fnExpProducer;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(argTypes);
        result = 31 * result + Boolean.hashCode(varArgs);
        return result;
    }

    public static class Builder {

        String name;
        TypeClassifier returnType;
        TypeClassifier[] argTypes;
        boolean varArgs;
        Function<List<Exp<?>>, Exp<?>> fnExpProducer;

        private Builder() {
        }

        public Builder udf0(Udf0<?> function) {
            fnExpProducer = exps
                    -> function.call();
            inferTypes(getCallMethodSafe(function, 0), false);
            return this;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder udf1(Udf1<?, ?> function) {
            fnExpProducer = exps
                    -> function.call((Exp)exps.getFirst());
            inferTypes(getCallMethodSafe(function, 1), false);
            return this;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder udf2(Udf2<?, ?, ?> function) {
            fnExpProducer = exps
                    -> function.call((Exp)exps.getFirst(), (Exp)exps.get(1));
            inferTypes(getCallMethodSafe(function, 2), false);
            return this;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder udf3(Udf3<?, ?, ?, ?> function) {
            fnExpProducer = exps
                    -> function.call((Exp)exps.getFirst(), (Exp)exps.get(1), (Exp)exps.get(2));
            inferTypes(getCallMethodSafe(function, 3), false);
            return this;
        }

        public Builder udfN(UdfN<?> function) {
            fnExpProducer = exps -> function.call(exps.toArray(new Exp[0]));
            inferTypes(getCallMethodSafe(function, 4), true);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        QLFunctionDescriptor build() {
            return new QLFunctionDescriptor(name, returnType, argTypes, varArgs, fnExpProducer);
        }

        private void inferTypes(Method method, boolean varArgs) {
            Type genericReturnType = method.getGenericReturnType();
            this.returnType = TypeClassifier.classify(genericReturnType);
            this.varArgs = varArgs;
            if(varArgs) {
                this.argTypes = new TypeClassifier[0];
            } else {
                Type[] genericParameterTypes = method.getGenericParameterTypes();
                this.argTypes = new TypeClassifier[genericParameterTypes.length];
                for (int i = 0; i < genericParameterTypes.length; i++) {
                    this.argTypes[i] = TypeClassifier.classify(genericParameterTypes[i]);
                }
            }
        }
    }

    public enum TypeClassifier {
        NUMERIC,
        STRING,
        BOOLEAN,
        DATE,
        TIME,
        DATETIME,

        /**
         * A type that is known to be a plain Object: an explicitly Object-typed scalar, including a null literal.
         * On the declaration side this is a wildcard that accepts an argument of any type.
         */
        OBJECT,

        /**
         * A type that is not known statically and is only resolved at eval time, e.g. an untyped column reference.
         * Produced by {@link #classify(Exp)} only.
         */
        ANY;

        /**
         * A {@link #matchCost(TypeClassifier, TypeClassifier)} result indicating that an argument can not be passed
         * to such a parameter.
         */
        public static final int NO_MATCH = -1;

        public static TypeClassifier classify(Type type) {
            Class<?> expressionType = unwindGeneric(type);
            if (Number.class.isAssignableFrom(expressionType)) {
                return NUMERIC;
            } else if (CharSequence.class.isAssignableFrom(expressionType)) {
                return STRING;
            } else if (expressionType.equals(Boolean.class)
                    || expressionType.equals(boolean.class)) {
                return BOOLEAN;
            } else if (expressionType.equals(java.time.LocalDate.class)) {
                return DATE;
            } else if (expressionType.equals(java.time.LocalTime.class)) {
                return TIME;
            } else if (expressionType.equals(java.time.LocalDateTime.class)
                    || expressionType.equals(java.time.OffsetDateTime.class)) {
                return DATETIME;
            } else {
                return OBJECT;
            }
        }

        public static TypeClassifier classify(Exp<?> exp) {
            return switch (exp) {
                case NumExp<?> ignored -> TypeClassifier.NUMERIC;
                case StrExp ignored -> TypeClassifier.STRING;
                case Condition ignored -> TypeClassifier.BOOLEAN;
                case DateExp ignored -> TypeClassifier.DATE;
                case TimeExp ignored -> TypeClassifier.TIME;
                case DateTimeExp ignored -> TypeClassifier.DATETIME;
                case OffsetDateTimeExp ignored -> TypeClassifier.DATETIME;
                case ScalarExp<?> ignored -> TypeClassifier.OBJECT;
                // anything else still typed as Object (a bare column ref, "if", "ifNull", "shift", ...) is only
                // resolved at eval time, so it is compatible with a parameter of any type
                case Exp<?> e when e.getType() == Object.class -> TypeClassifier.ANY;
                case null, default -> TypeClassifier.OBJECT;
            };
        }

        /**
         * Returns the cost of passing an argument of the {@code actual} type to a parameter declared as
         * {@code declared}. Returns {@link #NO_MATCH} if an argument can not be passed at all.
         */
        public static int matchCost(TypeClassifier declared, TypeClassifier actual) {

            if (declared == actual) {
                return 0; // an exact match
            }

            if (declared == OBJECT) {
                return 1; // the parameter is declared to accept an argument of any type
            }

            if (actual == ANY) {
                return 2; // the argument type is only known at eval time
            }

            return NO_MATCH;
        }
    }

    static private Method getCallMethodSafe(Object function, int arity) {
        Class<?> aClass = function.getClass();
        try {
            return switch (arity) {
                case 0 -> aClass.getDeclaredMethod("call");
                case 1 -> aClass.getDeclaredMethod("call", Exp.class);
                case 2 -> aClass.getDeclaredMethod("call", Exp.class, Exp.class);
                case 3 -> aClass.getDeclaredMethod("call", Exp.class, Exp.class, Exp.class);
                default -> aClass.getDeclaredMethod("call", Exp[].class);
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> unwindGeneric(Type type) {
        switch (type) {
            case Class<?> c -> {
                return c;
            }
            case ParameterizedType pt -> {
                Type[] actualTypeArguments = pt.getActualTypeArguments();
                return unwindGeneric(actualTypeArguments[0]);
            }
            case GenericArrayType gat -> {
                Type genericComponentType = gat.getGenericComponentType();
                return unwindGeneric(genericComponentType);
            }
            case WildcardType wt -> {
                Type[] lowerBounds = wt.getLowerBounds();
                if (lowerBounds.length > 0) {
                    return unwindGeneric(lowerBounds[0]);
                }
                Type[] upperBounds = wt.getUpperBounds();
                if (upperBounds.length > 0) {
                    return unwindGeneric(upperBounds[0]);
                }
                throw new IllegalArgumentException("Wildcard type with no bounds");
            }
            case TypeVariable<?> tv
                    -> throw new RuntimeException("Variable type " + tv + " can't be fully resolved");
            case null, default
                    -> throw new IllegalArgumentException("Unexpected type " + type);
        }
    }
}
