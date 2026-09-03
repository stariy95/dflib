package org.dflib.ql;

import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import java.util.ArrayList;
import java.util.Arrays;
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

import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.COERCION;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NO_MATCH;
import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.WILDCARD;

/**
 * @since 2.0.0
 */
public class QLFunctions {

    private final Map<String, SequencedSet<QLFunctionDescriptor>> functions;

    /**
     * Precomputed union of the return types of the overloads of each name, so that the parser can ask "may this
     * name return T?" in O(1) on every call site it considers.
     */
    private final Map<String, EnumSet<TypeClassifier>> returnTypes;

    /**
     * Names whose overloads disagree on their return type, precomputed for the same reason.
     */
    private final Set<String> polymorphicFunctions;

    /**
     * Names that may return one of the {@link TypeClassifier#isTyped() typed} classifiers, precomputed so that the
     * parser can ask in O(1) whether a typed expression rule claims a call site.
     */
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
     * Starts a builder of a function registry. The registry it builds includes all the built-in QL functions, so
     * that a custom function can be added to the language without taking anything away from it. Call
     * {@link Builder#noDefaultFunctions()} to build a registry of the explicitly registered functions only.
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
     * Returns true if a call to a function with this name may produce an expression of the given type. This is an
     * over-approximation that ignores the arguments: it is meant for the parser to decide which expression rule a
     * call can be parsed by, before the arguments are known.
     */
    public boolean mayReturn(String fnName, TypeClassifier type) {
        EnumSet<TypeClassifier> types = returnTypes.get(fnName);
        return types != null && types.contains(type);
    }

    /**
     * Returns true if a call to this name may produce an expression of one of the types the grammar has a dedicated
     * rule for. Such a name is claimed by that rule, and - unless it is also
     * {@link #isPolymorphicFn(String) polymorphic} - is not resolved through the untyped expression position at all.
     */
    public boolean hasTypedReturn(String fnName) {
        return typedReturnFunctions.contains(fnName);
    }

    /**
     * Returns true if the return type of a call to this name is not determined by the name alone, i.e. different
     * overloads of it return different types.
     */
    public boolean isPolymorphicFn(String fnName) {
        return polymorphicFunctions.contains(fnName);
    }

    /**
     * Resolves a function by its name and actual argument types. Overloads are ranked by preferring a fixed arity
     * over varargs, then by the most specific argument match, and finally by registration order. A tie that is only
     * a tie because an argument type is unknown until eval time is reported as ambiguous rather than resolved
     * arbitrarily.
     */
    public QLFunctionDescriptor function(String name, List<Arg> args) {

        SequencedSet<QLFunctionDescriptor> descriptors = functions.get(name);
        if (descriptors == null) {
            throw notFound(name, args);
        }

        // collect every candidate that is equally good, as such a tie may be an unresolvable ambiguity rather than
        // a registration-order question
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

        // an ambiguity the argument types can not explain is resolved in favor of the overload registered first
        return best.getFirst();
    }

    /**
     * How well a list of arguments fits a descriptor, lower being more specific. Compared in the order of the
     * components: a fixed arity beats varargs; then the fewer arguments passed to a typed parameter with their type
     * only known at eval time the better, as such an argument may still be rejected by the producer; and only then
     * the fewer arguments passed to an OBJECT parameter the better. The order of the last two is what makes an
     * overload written to accept any type win over one that would only pass a typed check at eval time, however
     * many of the other arguments the typed one matches exactly.
     */
    private record MatchCost(boolean varArgs, int coercions, int wildcards) implements Comparable<MatchCost> {

        private static final Comparator<MatchCost> ORDER = Comparator
                .comparing(MatchCost::varArgs)
                .thenComparingInt(MatchCost::coercions)
                .thenComparingInt(MatchCost::wildcards);

        /**
         * Returns the cost of passing the arguments to the descriptor, or null if they can not be passed at all.
         */
        static MatchCost of(QLFunctionDescriptor descriptor, List<Arg> args) {

            int declared = descriptor.args().length;

            if (descriptor.isVarArgs()) {
                // declared args of a vararg function are its leading typed parameters, and must all be present.
                // Anything past them is unconstrained
                if (args.size() < declared) {
                    return null;
                }
            } else if (declared != args.size()) {
                return null;
            }

            int coercions = 0;
            int wildcards = 0;

            for (int i = 0; i < declared; i++) {
                switch (Arg.matchCost(descriptor.args()[i], args.get(i))) {
                    case NO_MATCH -> {
                        return null;
                    }
                    case COERCION -> coercions++;
                    case WILDCARD -> wildcards++;
                    default -> {
                    }
                }
            }

            return new MatchCost(descriptor.isVarArgs(), coercions, wildcards);
        }

        @Override
        public int compareTo(MatchCost o) {
            return ORDER.compare(this, o);
        }
    }

    private static IllegalArgumentException notFound(String name, List<Arg> args) {
        return new IllegalArgumentException("Function " + name + "(" + args + ") not found");
    }

    /**
     * Throws if equally specific candidates disagree on the declared type of a parameter whose actual argument is
     * {@link TypeClassifier#ANY}. Such an argument matches every one of them at the same cost, so the choice would
     * come down to registration order - silently picking one overload for an expression whose type is only known at
     * eval time. The caller has to say which one it means.
     */
    private static void checkAmbiguity(String name, List<Arg> args, List<QLFunctionDescriptor> candidates) {

        int len = args.size();
        for (int i = 0; i < len; i++) {

            if (args.get(i).type() != TypeClassifier.ANY) {
                continue;
            }

            EnumSet<TypeClassifier> declared = EnumSet.noneOf(TypeClassifier.class);
            for (QLFunctionDescriptor d : candidates) {
                // a vararg candidate may declare fewer parameters than the call passes
                if (i < d.args().length) {
                    declared.add(d.args()[i].type());
                }
            }

            if (declared.size() > 1) {
                throw new IllegalArgumentException("Ambiguous call to " + name + "(): the type of argument "
                        + (i + 1) + " is only known at eval time, and " + name + " is defined for " + declared
                        + " arguments in that position. " + castHint(name, declared));
            }
        }
    }

    /**
     * The cast a caller would wrap an untyped argument in to pick one of the ambiguous overloads. Names one of the
     * declared types, so the hint is a call the user can paste.
     */
    private static String castHint(String name, EnumSet<TypeClassifier> declared) {

        for (TypeClassifier t : declared) {
            if (t.isTyped()) {
                return "Cast it, e.g. " + name + "(" + t.castFunction() + "(..))";
            }
        }

        return "Cast it to one of them.";
    }

    /**
     * Returns all registered descriptors.
     */
    Stream<QLFunctionDescriptor> descriptors() {
        return functions.values().stream().flatMap(Collection::stream);
    }

    /**
     * A builder of a function registry. Unless {@link #noDefaultFunctions()} is called, the registry it builds
     * includes all the built-in QL functions in addition to the explicitly registered ones. A function whose name
     * and argument types are the same as those of another function - a built-in one included - can not be resolved
     * by the parser, so registering one is an error reported from {@link #build()}.
     */
    public static class Builder {

        private final Map<String, SequencedSet<QLFunctionDescriptor>> functions = new LinkedHashMap<>();

        private boolean defaultFunctions = true;

        private Builder() {
        }

        public Builder function(String name, Udf0<?> function) {
            return function(name, QLFunctionSignature.udf0(function));
        }

        public Builder function(String name, Udf1<?, ?> function) {
            return function(name, QLFunctionSignature.udf1(function));
        }

        public Builder function(String name, Udf2<?, ?, ?> function) {
            return function(name, QLFunctionSignature.udf2(function));
        }

        public Builder function(String name, Udf3<?, ?, ?, ?> function) {
            return function(name, QLFunctionSignature.udf3(function));
        }

        public Builder function(String name, UdfN<?> function) {
            return function(name, QLFunctionSignature.udfN(function));
        }

        /**
         * Registers a function implemented as a class of typed {@code call} overloads. Every public {@code call}
         * method declared in the class becomes one signature of the function, so a single call registers a whole
         * overload set - one per receiver type and arity. See {@link QLFunction} for the rules such a class must
         * follow; violating any of them is reported from here.
         */
        public Builder function(String name, QLFunction function) {
            for (QLFunctionSignature s : QLFunctionSignature.reflectQLFunction(name, function)) {
                function(name, s);
            }

            return this;
        }

        /**
         * Registers a function described by an explicit signature. Package-private for now: this is how the
         * registry's tests declare functions of an arbitrary shape.
         */
        Builder function(String name, QLFunctionSignature signature) {
            return defineFunction(name, new QLFunctionDescriptor(name, signature));
        }

        private Builder defineFunction(String name, QLFunctionDescriptor descriptor) {
            boolean hasSameDescriptor = !functions.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(descriptor);
            if (hasSameDescriptor) {
                throw new IllegalArgumentException("Function " + name + "(" + Arrays.toString(descriptor.args()) + ") already defined");
            }

            return this;
        }

        /**
         * Includes the built-in QL functions in the registry being built. They are included by default, so this call
         * only has an effect after {@link #noDefaultFunctions()}.
         */
        public Builder defaultFunctions() {
            this.defaultFunctions = true;
            return this;
        }

        /**
         * Excludes the built-in QL functions from the registry being built. The result is a registry of the
         * explicitly registered functions only, which replaces - rather than extends - the QL function vocabulary.
         * Note that the grammar rules that are not function calls (column references, operators, literals,
         * {@code array(..)}) are unaffected by the registry and remain available.
         */
        public Builder noDefaultFunctions() {
            this.defaultFunctions = false;
            return this;
        }

        public QLFunctions build() {

            // built-ins go in first, so that among equally specific overloads they are preferred, matching the
            // order they are declared in. A side effect is that a custom function colliding with a built-in one is
            // reported here rather than at registration time
            Builder all = new Builder().noDefaultFunctions();
            if (defaultFunctions) {
                DefaultQLFunctions.register(all);
            }

            functions.forEach((name, descriptors)
                    -> descriptors.forEach(d -> all.defineFunction(name, d)));
            return new QLFunctions(all.functions);
        }
    }
}
