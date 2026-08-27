package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A fluent, non-reflective description of a QL function: its return type, its declared parameters and the factory
 * that turns a list of argument expressions into an expression.
 * <p>
 * This is the single way a {@link QLFunctionDescriptor} is assembled. The reflective {@code Udf0..UdfN} registration
 * path is implemented on top of it via {@link #reflect(Method, boolean)}, so there is only one descriptor
 * construction code path.
 * <p>
 * Package-private on purpose: built-in functions are registered through it, and it is only promoted to public API
 * if and when users need to register functions whose shape reflection can not express.
 */
class QLFunctionSignature {

    /**
     * A {@link #returnArgIndex} value meaning "the return type does not depend on the arguments".
     */
    static final int FIXED_RETURN = -1;

    private TypeClassifier returnType;
    private int returnArgIndex = FIXED_RETURN;
    private final List<Arg> args = new ArrayList<>();
    private boolean varArgs;
    private Function<List<Exp<?>>, Exp<?>> producer;

    static QLFunctionSignature signature() {
        return new QLFunctionSignature();
    }

    /**
     * Builds a signature by reflecting on a UDF "call" method. Parameter and return types are recovered from the
     * method generics; {@link Constant}-annotated parameters become constant args.
     *
     * @param varArgs whether the method's single {@code Exp[]} parameter is a vararg list rather than a declared
     *                parameter
     */
    static QLFunctionSignature reflect(Method method, boolean varArgs) {

        QLFunctionSignature signature = signature()
                .returning(TypeClassifier.classify(method.getGenericReturnType()));

        Parameter[] parameters = method.getParameters();

        if (varArgs) {
            // varargs declare no individual arguments, so a constant marker on them would be silently dropped
            for (Parameter p : parameters) {
                if (p.isAnnotationPresent(Constant.class)) {
                    throw new IllegalArgumentException(
                            "Vararg functions declare no arguments, so none can be @Constant: " + method);
                }
            }

            signature.varArgs();
        } else {
            for (Parameter p : parameters) {
                signature.arg(Arg.of(p));
            }
        }

        return signature;
    }

    private QLFunctionSignature() {
    }

    /**
     * Declares a return type that does not depend on the arguments.
     */
    QLFunctionSignature returning(TypeClassifier type) {
        this.returnType = type;
        this.returnArgIndex = FIXED_RETURN;
        return this;
    }

    /**
     * Declares a polymorphic function whose return type is the type of the argument at the given position, e.g.
     * {@code shift(e, n)} or {@code min(e)}.
     */
    QLFunctionSignature returningArgType(int argIndex) {
        if (argIndex < 0) {
            throw new IllegalArgumentException("Negative return arg index: " + argIndex);
        }
        this.returnType = null;
        this.returnArgIndex = argIndex;
        return this;
    }

    /**
     * Appends a declared parameter that accepts any expression of the given type.
     */
    QLFunctionSignature arg(TypeClassifier type) {
        return arg(new Arg(type, false));
    }

    /**
     * Appends a declared parameter that only accepts a constant (scalar) expression of the given type. Such
     * arguments can be read at parse time.
     */
    QLFunctionSignature constArg(TypeClassifier type) {
        return arg(new Arg(type, true));
    }

    QLFunctionSignature arg(Arg arg) {
        this.args.add(arg);
        return this;
    }

    /**
     * Marks the function as accepting a variable number of trailing arguments. Any parameters declared with
     * {@link #arg(TypeClassifier)} / {@link #constArg(TypeClassifier)} become leading typed parameters that must
     * still be present and match; anything past them is unconstrained.
     */
    QLFunctionSignature varArgs() {
        this.varArgs = true;
        return this;
    }

    /**
     * Sets the factory producing an expression from the call arguments.
     */
    QLFunctionSignature as(Function<List<Exp<?>>, Exp<?>> producer) {
        this.producer = producer;
        return this;
    }

    TypeClassifier returnType() {
        return returnType;
    }

    int returnArgIndex() {
        return returnArgIndex;
    }

    Arg[] args() {
        return args.toArray(new Arg[0]);
    }

    boolean isVarArgs() {
        return varArgs;
    }

    Function<List<Exp<?>>, Exp<?>> producer() {
        return producer;
    }

    void validate(String name) {
        if (producer == null) {
            throw new IllegalArgumentException("No expression producer defined for function: " + name);
        }

        if (returnArgIndex == FIXED_RETURN) {
            if (returnType == null) {
                throw new IllegalArgumentException("No return type defined for function: " + name);
            }
        } else if (returnArgIndex >= args.size()) {
            throw new IllegalArgumentException("Function " + name + " returns the type of argument "
                    + returnArgIndex + ", but declares only " + args.size() + " argument(s)");
        }
    }
}
