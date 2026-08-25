package org.dflib.ql;

import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;
import org.dflib.exp.fn.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NO_MATCH;

public class QLFunctions {

    private final Map<String, SequencedSet<QLFunctionDescriptor>> functions;

    private QLFunctions(Map<String, SequencedSet<QLFunctionDescriptor>> functions) {
        this.functions = functions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean strFn(String fnName) {
        return descriptorsForTypeAndName(fnName, QLFunctionDescriptor.TypeClassifier.STRING)
                .findAny()
                .isPresent();
    }

    public boolean numFn(String fnName) {
        return descriptorsForTypeAndName(fnName, QLFunctionDescriptor.TypeClassifier.NUMERIC)
                .findAny()
                .isPresent();
    }

    public QLFunctionDescriptor function(String name, QLFunctionDescriptor.TypeClassifier type, List<QLFunctionDescriptor.TypeClassifier> argTypes) {
        return descriptorsForTypeAndName(name, type)
                // TODO: polymorphic functions support
                .filter(d -> matchCost(d, argTypes) != NO_MATCH)
                // prefer fixed arity over varargs, then the most specific match
                .min(Comparator.comparing(QLFunctionDescriptor::isVarArgs)
                        .thenComparingInt(d -> matchCost(d, argTypes)))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Function " + type + " " + name + "(" + argTypes + ") not found"
                ));
    }

    /**
     * Returns the combined cost of passing the given arguments to the descriptor parameters. The lower the cost, the
     * more specific the match. Returns {@link QLFunctionDescriptor.TypeClassifier#NO_MATCH} if the arguments can not
     * be passed to this function at all.
     */
    private static int matchCost(QLFunctionDescriptor descriptor, List<QLFunctionDescriptor.TypeClassifier> argTypes) {

        // varargs declare no parameters, so they accept an argument list of any length
        if (descriptor.argTypes().length != argTypes.size()) {
            return descriptor.isVarArgs() ? 0 : NO_MATCH;
        }

        int cost = 0;
        for (int i = 0; i < descriptor.argTypes().length; i++) {
            int argCost = QLFunctionDescriptor.TypeClassifier.matchCost(descriptor.argTypes()[i], argTypes.get(i));
            if (argCost == NO_MATCH) {
                return NO_MATCH;
            }

            cost += argCost;
        }

        return cost;
    }

    Stream<QLFunctionDescriptor> descriptorsForTypeAndName(String name, QLFunctionDescriptor.TypeClassifier type) {
        SequencedSet<QLFunctionDescriptor> descriptors = functions.get(name);
        return descriptors != null
                ? descriptors.stream().filter(d -> d.returnType() == type)
                : Stream.empty();
    }

    public static class Builder {

        private final Map<String, SequencedSet<QLFunctionDescriptor>> functions = new ConcurrentHashMap<>();

        private Builder() {
        }

        public Builder function(String name, Udf0<?> function) {
            return defineFunction(name, QLFunctionDescriptor.ofUdf0(function));
        }

        public Builder function(String name, Udf1<?, ?> function) {
            return defineFunction(name, QLFunctionDescriptor.ofUdf1(function));
        }

        public Builder function(String name, Udf2<?, ?, ?> function) {
            return defineFunction(name, QLFunctionDescriptor.ofUdf2(function));
        }

        public Builder function(String name, Udf3<?, ?, ?, ?> function) {
            return defineFunction(name, QLFunctionDescriptor.ofUdf3(function));
        }

        public Builder function(String name, UdfN<?> function) {
            return defineFunction(name, QLFunctionDescriptor.ofUdfN(function));
        }

        private Builder defineFunction(String name, QLFunctionDescriptor.Builder builder) {
            QLFunctionDescriptor descriptor = builder.name(name).build();
            boolean hasSameDescriptor = !functions.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(descriptor);
            if(hasSameDescriptor) {
                throw new IllegalArgumentException("Function " + name + "(" + Arrays.toString(descriptor.argTypes()) + ")  already defined");
            }

            return this;
        }

        public Builder defaultFunctions() {
            return this.function("trim", new TrimFunction())
                    .function("lower", new LowerFunction())
                    .function("upper", new UpperFunction())
                    .function("substr", new Substr2Function())
                    .function("substr", new Substr3Function())
                    .function("len", new LenFunction())
                    .function("abs", new AbsFunction())
                    .function("sqrt", new SqrtFunction())
                    .function("round", new RoundFunction())
                    .function("rowNum", new RowNumFunction());
        }

        public QLFunctions build() {
            return new QLFunctions(functions);
        }
    }
}
