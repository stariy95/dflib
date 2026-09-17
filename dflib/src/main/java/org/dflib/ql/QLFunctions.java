package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dflib.ql.TypeClassifier.NO_MATCH;
import static org.dflib.ql.TypeClassifier.WILDCARD;

/**
 * A registry of functions recognized by the QL parser. A function call is resolved by name and by the types of its
 * arguments: an argument must be an expression of the declared type ({@code max(int(a))}), or of any type if the
 * parameter is declared as a plain {@code Exp} ({@code trim(a)}). An untyped expression, such as a bare column
 * reference, has to be cast explicitly to be passed to a typed parameter ({@code max(castAsInt(a))}).
 *
 * @since 2.0.0
 */
public class QLFunctions {

    private final Map<String, SequencedSet<QLFunctionDescriptor>> functions;

    private QLFunctions(Map<String, SequencedSet<QLFunctionDescriptor>> functions) {
        this.functions = functions;
    }

    /**
     * Starts a builder of a registry that includes the built-in functions, unless
     * {@link Builder#noDefaultFunctions()} is called.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolves a function call by its name and argument types and builds the call expression.
     *
     * @throws IllegalArgumentException if the function is unknown, no overload matches the arguments, or the
     *                                  matching overload rejects them
     */
    public Exp<?> call(String name, List<Exp<?>> args) {
        return function(name, args.stream().map(QLFunctionArg::of).toList()).expProducer().apply(args);
    }

    /**
     * Resolves a function by its name and argument types, preferring a fixed arity over varargs, then the fewest
     * arguments passed to untyped parameters, then the registration order.
     */
    QLFunctionDescriptor function(String name, List<QLFunctionArg> args) {

        SequencedSet<QLFunctionDescriptor> descriptors = functions.get(name);
        if (descriptors == null) {
            throw new IllegalArgumentException("Unknown function: " + name);
        }

        QLFunctionDescriptor best = null;
        MatchCost bestCost = null;

        for (QLFunctionDescriptor d : descriptors) {
            MatchCost cost = MatchCost.of(d, args);
            if (cost != null && (bestCost == null || cost.compareTo(bestCost) < 0)) {
                best = d;
                bestCost = cost;
            }
        }

        if (best == null) {
            throw notFound(name, args, descriptors);
        }

        return best;
    }

    Stream<QLFunctionDescriptor> descriptors() {
        return functions.values().stream().flatMap(Collection::stream);
    }

    /**
     * How well the arguments fit a descriptor, lower being better: a fixed arity beats varargs, then fewer
     * arguments passed to untyped parameters.
     */
    private record MatchCost(boolean varArgs, int wildcards) implements Comparable<MatchCost> {

        private static final Comparator<MatchCost> ORDER = Comparator
                .comparing(MatchCost::varArgs)
                .thenComparingInt(MatchCost::wildcards);

        static MatchCost of(QLFunctionDescriptor descriptor, List<QLFunctionArg> args) {

            int declared = descriptor.args().size();

            if (descriptor.varArgs() ? args.size() < declared : args.size() != declared) {
                return null;
            }

            int wildcards = 0;
            for (int i = 0; i < declared; i++) {
                switch (QLFunctionArg.matchCost(descriptor.args().get(i), args.get(i))) {
                    case NO_MATCH -> {
                        return null;
                    }
                    case WILDCARD -> wildcards++;
                    default -> {
                    }
                }
            }

            return new MatchCost(descriptor.varArgs(), wildcards);
        }

        @Override
        public int compareTo(MatchCost o) {
            return ORDER.compare(this, o);
        }
    }

    /**
     * Builds an error listing the registered overloads, and for an untyped argument in a position where the
     * overloads expect typed ones, the casts that would make it match.
     */
    private static IllegalArgumentException notFound(
            String name,
            List<QLFunctionArg> args,
            Collection<QLFunctionDescriptor> descriptors) {

        StringBuilder message = new StringBuilder("No overload of ")
                .append(name)
                .append(" matches ")
                .append(QLFunctionDescriptor.shape(name, args, false))
                .append(". Available: ")
                .append(descriptors.stream().map(QLFunctionDescriptor::shape).collect(Collectors.joining(", ")));

        for (int i = 0; i < args.size(); i++) {

            if (args.get(i).type().isTyped()) {
                continue;
            }

            EnumSet<TypeClassifier> expected = EnumSet.noneOf(TypeClassifier.class);
            for (QLFunctionDescriptor d : descriptors) {
                if (i < d.args().size() && d.args().get(i).type().isTyped()) {
                    expected.add(d.args().get(i).type());
                }
            }

            if (!expected.isEmpty()) {
                message.append(". Argument ")
                        .append(i + 1)
                        .append(" is untyped; cast it to one of ")
                        .append(expected)
                        .append(", e.g. ")
                        .append(expected.iterator().next().castFunction())
                        .append("(..)");
            }
        }

        return new IllegalArgumentException(message.toString());
    }

    /**
     * A builder of a function registry. A function is registered as a {@link QLFunction} class, or as a
     * {@code Udf0..UdfN} object; a lambda needs a type to be reflected on, so it is registered via
     * {@code Udf1.of(e -> ..)}, {@code Udf2.of(..)}, etc. Registering a function with the same name and argument
     * types as another one, a built-in included, is an error reported from {@link #build()}.
     */
    public static class Builder {

        private final Map<String, SequencedSet<QLFunctionDescriptor>> functions = new LinkedHashMap<>();

        private boolean defaultFunctions = true;

        private Builder() {
        }

        public Builder function(String name, Udf0<?> function) {
            return function(QLFunctionReflection.udf0(name, function));
        }

        public Builder function(String name, Udf1<?, ?> function) {
            return function(QLFunctionReflection.udf1(name, function));
        }

        public Builder function(String name, Udf2<?, ?, ?> function) {
            return function(QLFunctionReflection.udf2(name, function));
        }

        public Builder function(String name, Udf3<?, ?, ?, ?> function) {
            return function(QLFunctionReflection.udf3(name, function));
        }

        public Builder function(String name, UdfN<?> function) {
            return function(QLFunctionReflection.udfN(name, function));
        }

        /**
         * Registers every public {@code call} overload of a {@link QLFunction} class as an overload of the function.
         */
        public Builder function(String name, QLFunction function) {
            for (QLFunctionDescriptor d : QLFunctionReflection.qlFunction(name, function)) {
                function(d);
            }

            return this;
        }

        Builder function(QLFunctionDescriptor descriptor) {
            String name = descriptor.name();
            boolean added = functions.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(descriptor);
            if (!added) {
                throw new IllegalArgumentException("Function " + descriptor.shape() + " already defined");
            }

            return this;
        }

        /**
         * Excludes the built-in functions from the registry, leaving only the explicitly registered ones.
         */
        public Builder noDefaultFunctions() {
            this.defaultFunctions = false;
            return this;
        }

        public QLFunctions build() {

            // built-ins go first, so that they win ties with custom functions
            Builder builder = new Builder().noDefaultFunctions();
            if (defaultFunctions) {
                DefaultQLFunctions.register(builder);
            }

            functions.values().forEach(descriptors -> descriptors.forEach(builder::function));
            return new QLFunctions(builder.functions);
        }
    }
}
