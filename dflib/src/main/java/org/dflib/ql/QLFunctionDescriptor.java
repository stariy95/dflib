package org.dflib.ql;

import org.dflib.Exp;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A description of one overload of a QL function: its name, argument shape, return type and expression factory.
 *
 * @since 2.0.0
 */
public record QLFunctionDescriptor(String name, TypeClassifier returnType,
                                   List<QLFunctionArg> args, boolean varArgs,
                                   Function<List<Exp<?>>, Exp<?>> expProducer) {

    public QLFunctionDescriptor(
            String name,
            TypeClassifier returnType,
            List<QLFunctionArg> args,
            boolean varArgs,
            Function<List<Exp<?>>, Exp<?>> expProducer) {

        if (returnType == null) {
            throw new IllegalArgumentException("No return type defined for function: " + name);
        }

        if (expProducer == null) {
            throw new IllegalArgumentException("No expression producer defined for function: " + name);
        }

        this.name = name;
        this.returnType = returnType;
        this.args = List.copyOf(args);
        this.varArgs = varArgs;
        this.expProducer = expProducer;
    }

    /**
     * Descriptors with the same name and argument shape are equal regardless of the return type, as the parser
     * can not tell them apart.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        QLFunctionDescriptor that = (QLFunctionDescriptor) o;
        return name.equals(that.name)
                && varArgs == that.varArgs
                && args.equals(that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, args, varArgs);
    }
}
