package org.dflib.ql;

import org.dflib.Udf0;
import org.dflib.Udf1;
import org.dflib.Udf2;
import org.dflib.Udf3;
import org.dflib.UdfN;
import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SequencedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.dflib.ql.QLFunctionDescriptor.TypeClassifier.NO_MATCH;

public class QLFunctions {

    /**
     * The return types that a typed expression rule of the grammar can ask about. A function that may return one of
     * these is claimed by a typed rule; anything else ({@code OBJECT}, {@code ANY}) is only reachable from the
     * untyped expression position.
     */
    private static final EnumSet<TypeClassifier> TYPED_RETURNS = EnumSet.of(
            TypeClassifier.NUMERIC,
            TypeClassifier.STRING,
            TypeClassifier.BOOLEAN,
            TypeClassifier.DATE,
            TypeClassifier.TIME,
            TypeClassifier.DATETIME,
            TypeClassifier.OFFSETDATETIME);

    private final Map<String, SequencedSet<QLFunctionDescriptor>> functions;

    /**
     * Precomputed union of {@link QLFunctionDescriptor#possibleReturnTypes()} per function name, so that the parser
     * can ask "may this name return T?" in O(1) on every call site it considers.
     */
    private final Map<String, EnumSet<TypeClassifier>> returnTypes;

    private final Set<String> polymorphicFunctions;

    /**
     * Names that may return one of the {@link #TYPED_RETURNS}, precomputed so that the parser can ask in O(1)
     * whether a typed expression rule claims a call site.
     */
    private final Set<String> typedReturnFunctions;

    private QLFunctions(Map<String, SequencedSet<QLFunctionDescriptor>> functions) {
        this.functions = functions;
        this.returnTypes = returnTypes(functions);
        this.polymorphicFunctions = polymorphicFunctions(functions);
        this.typedReturnFunctions = typedReturnFunctions(this.returnTypes);
    }

    private static Set<String> typedReturnFunctions(Map<String, EnumSet<TypeClassifier>> returnTypes) {

        Set<String> typed = new HashSet<>();
        for (Map.Entry<String, EnumSet<TypeClassifier>> e : returnTypes.entrySet()) {
            if (!Collections.disjoint(e.getValue(), TYPED_RETURNS)) {
                typed.add(e.getKey());
            }
        }

        return typed;
    }

    private static Map<String, EnumSet<TypeClassifier>> returnTypes(
            Map<String, SequencedSet<QLFunctionDescriptor>> functions) {

        Map<String, EnumSet<TypeClassifier>> types = new HashMap<>();
        for (Map.Entry<String, SequencedSet<QLFunctionDescriptor>> e : functions.entrySet()) {
            EnumSet<TypeClassifier> merged = EnumSet.noneOf(TypeClassifier.class);
            for (QLFunctionDescriptor d : e.getValue()) {
                merged.addAll(d.possibleReturnTypes());
            }
            types.put(e.getKey(), merged);
        }

        return types;
    }

    private static Set<String> polymorphicFunctions(Map<String, SequencedSet<QLFunctionDescriptor>> functions) {

        Set<String> polymorphic = new HashSet<>();

        for (Map.Entry<String, SequencedSet<QLFunctionDescriptor>> e : functions.entrySet()) {

            Set<TypeClassifier> fixed = EnumSet.noneOf(TypeClassifier.class);
            boolean isPolymorphic = false;

            for (QLFunctionDescriptor d : e.getValue()) {
                if (d.returnArgIndex() != QLFunctionSignature.FIXED_RETURN) {
                    isPolymorphic = true;
                    break;
                }
                fixed.add(d.returnType());
            }

            if (isPolymorphic || fixed.size() > 1) {
                polymorphic.add(e.getKey());
            }
        }

        return polymorphic;
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
     * Returns true if the return type of a call to this name is not determined by the name alone: either some of
     * its overloads return the type of one of their arguments, or different overloads return different types.
     */
    public boolean isPolymorphicFn(String fnName) {
        return polymorphicFunctions.contains(fnName);
    }

    /**
     * Resolves a function by its name and actual argument types. Overloads are ranked by preferring a fixed arity
     * over varargs, then by the most specific argument match, and finally by registration order.
     */
    public QLFunctionDescriptor function(String name, List<Arg> args) {
        SequencedSet<QLFunctionDescriptor> descriptors = functions.get(name);
        return (descriptors != null ? descriptors.stream() : Stream.<QLFunctionDescriptor>empty())
                .filter(d -> matchCost(d, args) != NO_MATCH)
                // prefer fixed arity over varargs, then the most specific match.
                // Equally specific overloads are resolved in favor of the one registered first.
                .min(Comparator.comparing(QLFunctionDescriptor::isVarArgs)
                        .thenComparingInt(d -> matchCost(d, args)))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Function " + name + "(" + args + ") not found"
                ));
    }

    /**
     * Returns the combined cost of passing the given arguments to the descriptor parameters. The lower the cost, the
     * more specific the match. Returns {@link QLFunctionDescriptor.TypeClassifier#NO_MATCH} if the arguments can not
     * be passed to this function at all.
     */
    private static int matchCost(QLFunctionDescriptor descriptor, List<Arg> args) {

        int declared = descriptor.args().length;

        if (descriptor.isVarArgs()) {
            // declared args of a vararg function are its leading typed parameters, and must all be present.
            // Anything past them is unconstrained
            if (args.size() < declared) {
                return NO_MATCH;
            }
        } else if (declared != args.size()) {
            return NO_MATCH;
        }

        int cost = 0;
        for (int i = 0; i < declared; i++) {
            int argCost = Arg.matchCost(descriptor.args()[i], args.get(i));
            if (argCost == NO_MATCH) {
                return NO_MATCH;
            }

            cost += argCost;
        }

        return cost;
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

        private final Map<String, SequencedSet<QLFunctionDescriptor>> functions = new ConcurrentHashMap<>();

        private boolean defaultFunctions = true;

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

        /**
         * Registers a function described by an explicit signature. Package-private for now: this is how built-in
         * functions are declared.
         */
        Builder function(String name, QLFunctionSignature signature) {
            return defineFunction(name, new QLFunctionDescriptor(name, signature));
        }

        private Builder defineFunction(String name, QLFunctionDescriptor.Builder builder) {
            return defineFunction(name, builder.name(name).build());
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

            functions.forEach((name, descriptors) -> descriptors.forEach(d -> all.defineFunction(name, d)));
            return new QLFunctions(all.functions);
        }
    }
}
