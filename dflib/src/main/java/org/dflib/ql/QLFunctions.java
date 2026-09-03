package org.dflib.ql;

import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SequencedSet;
import java.util.stream.Stream;

import static org.dflib.ql.TypeClassifier.COERCION;
import static org.dflib.ql.TypeClassifier.NO_MATCH;
import static org.dflib.ql.TypeClassifier.WILDCARD;

/**
 * A registry of functions recognized by the QL parser.
 *
 * @since 2.0.0
 */
public class QLFunctions {

    private final Map<String, SequencedSet<QLFunctionDescriptor>> functions;
    private final Map<String, EnumSet<TypeClassifier>> returnTypes;
    private final Set<String> polymorphicFunctions;
    private final Set<String> typedReturnFunctions;

    private QLFunctions(Map<String, SequencedSet<QLFunctionDescriptor>> functions) {
        this.functions = functions;
        this.returnTypes = new HashMap<>();
        this.polymorphicFunctions = new HashSet<>();
        this.typedReturnFunctions = new HashSet<>();

        for (Map.Entry<String, SequencedSet<QLFunctionDescriptor>> e : functions.entrySet()) {

            EnumSet<TypeClassifier> types = EnumSet.noneOf(TypeClassifier.class);
            for (QLFunctionDescriptor d : e.getValue()) {
                types.add(d.returnType());
            }

            returnTypes.put(e.getKey(), types);

            if (types.size() > 1) {
                polymorphicFunctions.add(e.getKey());
            }

            if (types.stream().anyMatch(TypeClassifier::isTyped)) {
                typedReturnFunctions.add(e.getKey());
            }
        }
    }

    /**
     * Starts a builder of a registry that includes the built-in functions, unless
     * {@link Builder#noDefaultFunctions()} is called.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if a function with this name is registered, regardless of its arity or return type.
     */
    public boolean isFn(String fnName) {
        return functions.containsKey(fnName);
    }

    /**
     * Returns true if some overload of the function may produce an expression of the given type.
     */
    public boolean mayReturn(String fnName, TypeClassifier type) {
        EnumSet<TypeClassifier> types = returnTypes.get(fnName);
        return types != null && types.contains(type);
    }

    /**
     * Returns true if some overload of the function returns one of the types the grammar has a dedicated rule for.
     */
    public boolean hasTypedReturn(String fnName) {
        return typedReturnFunctions.contains(fnName);
    }

    /**
     * Returns true if the overloads of the function return different types.
     */
    public boolean isPolymorphicFn(String fnName) {
        return polymorphicFunctions.contains(fnName);
    }

    /**
     * Resolves a function by its name and argument types, preferring a fixed arity over varargs, then the most
     * specific argument match, then the registration order. A tie caused by an argument whose type is only known
     * at eval time is reported as ambiguous.
     */
    public QLFunctionDescriptor function(String name, List<QLFunctionArg> args) {

        SequencedSet<QLFunctionDescriptor> descriptors = functions.get(name);
        if (descriptors == null) {
            throw notFound(name, args);
        }

        List<QLFunctionDescriptor> best = new ArrayList<>(2);
        MatchCost bestCost = null;

        for (QLFunctionDescriptor d : descriptors) {

            MatchCost cost = MatchCost.of(d, args);
            if (cost == null) {
                continue;
            }

            int cmp = bestCost == null ? -1 : cost.compareTo(bestCost);
            if (cmp < 0) {
                best.clear();
                best.add(d);
                bestCost = cost;
            } else if (cmp == 0) {
                best.add(d);
            }
        }

        if (best.isEmpty()) {
            throw notFound(name, args);
        }

        if (best.size() > 1) {
            checkAmbiguity(name, args, best);
        }

        return best.getFirst();
    }

    /**
     * How well the arguments fit a descriptor, lower being better: a fixed arity beats varargs, then fewer
     * eval-time coercions, then fewer wildcard matches.
     */
    private record MatchCost(boolean varArgs, int coercions, int wildcards) implements Comparable<MatchCost> {

        private static final Comparator<MatchCost> ORDER = Comparator
                .comparing(MatchCost::varArgs)
                .thenComparingInt(MatchCost::coercions)
                .thenComparingInt(MatchCost::wildcards);

        static MatchCost of(QLFunctionDescriptor descriptor, List<QLFunctionArg> args) {

            int declared = descriptor.args().size();

            if (descriptor.varArgs()) {
                if (args.size() < declared) {
                    return null;
                }
            } else if (declared != args.size()) {
                return null;
            }

            int coercions = 0;
            int wildcards = 0;

            for (int i = 0; i < declared; i++) {
                switch (QLFunctionArg.matchCost(descriptor.args().get(i), args.get(i))) {
                    case NO_MATCH -> {
                        return null;
                    }
                    case COERCION -> coercions++;
                    case WILDCARD -> wildcards++;
                    default -> {
                    }
                }
            }

            return new MatchCost(descriptor.varArgs(), coercions, wildcards);
        }

        @Override
        public int compareTo(MatchCost o) {
            return ORDER.compare(this, o);
        }
    }

    private static IllegalArgumentException notFound(String name, List<QLFunctionArg> args) {
        return new IllegalArgumentException("Function " + name + "(" + args + ") not found");
    }

    /**
     * Throws if equally good candidates disagree on the declared type of a parameter whose argument is ANY.
     */
    private static void checkAmbiguity(String name, List<QLFunctionArg> args, List<QLFunctionDescriptor> candidates) {

        int len = args.size();
        for (int i = 0; i < len; i++) {

            if (args.get(i).type() != TypeClassifier.ANY) {
                continue;
            }

            EnumSet<TypeClassifier> declared = EnumSet.noneOf(TypeClassifier.class);
            for (QLFunctionDescriptor d : candidates) {
                if (i < d.args().size()) {
                    declared.add(d.args().get(i).type());
                }
            }

            if (declared.size() > 1) {
                throw new IllegalArgumentException("Ambiguous call to " + name + "(): the type of argument "
                        + (i + 1) + " is only known at eval time, and " + name + " is defined for " + declared
                        + " arguments in that position. " + castHint(name, declared));
            }
        }
    }

    private static String castHint(String name, EnumSet<TypeClassifier> declared) {

        for (TypeClassifier t : declared) {
            if (t.isTyped()) {
                return "Cast it, e.g. " + name + "(" + t.castFunction() + "(..))";
            }
        }

        return "Cast it to one of them.";
    }

    Stream<QLFunctionDescriptor> descriptors() {
        return functions.values().stream().flatMap(Collection::stream);
    }

    /**
     * A builder of a function registry. Registering a function with the same name and argument types as another
     * one, a built-in included, is an error reported from {@link #build()}.
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
            boolean hasSameDescriptor = !functions.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(descriptor);
            if (hasSameDescriptor) {
                throw new IllegalArgumentException("Function " + name + "(" + descriptor.args() + ") already defined");
            }

            return this;
        }

        /**
         * Includes the built-in functions in the registry. This is the default.
         */
        public Builder defaultFunctions() {
            this.defaultFunctions = true;
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
            Builder all = new Builder().noDefaultFunctions();
            if (defaultFunctions) {
                DefaultQLFunctions.register(all);
            }

            functions.values().forEach(descriptors -> descriptors.forEach(all::function));
            return new QLFunctions(all.functions);
        }
    }
}
