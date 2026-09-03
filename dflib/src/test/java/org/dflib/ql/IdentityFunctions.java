package org.dflib.ql;

import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import static org.dflib.ql.QLFunctionSignature.signature;

/**
 * Registers stand-ins for the built-in functions whose return type is the type of their receiver - "shift",
 * "plusDays", "min" - so that dispatch tests can exercise a polymorphic call site against a registry of a known
 * shape, and a failure points at the dispatch rules rather than at a built-in's signature.
 */
class IdentityFunctions {

    private IdentityFunctions() {
    }

    /**
     * Registers a function returning its first argument unchanged, the way every built-in is written: one
     * fixed-return overload per typed receiver, plus one for a receiver whose type is only known at eval time. The
     * type of a call to it is the type of its receiver, and is not known from its name.
     */
    static QLFunctions.Builder identity(QLFunctions.Builder builder, String name, Arg... trailingArgs) {

        for (TypeClassifier t : TypeClassifier.values()) {
            if (t.isTyped()) {
                builder.function(name, overload(t, t, trailingArgs));
            }
        }

        return builder.function(name, overload(TypeClassifier.OBJECT, TypeClassifier.ANY, trailingArgs));
    }

    private static QLFunctionSignature overload(TypeClassifier receiver, TypeClassifier returns, Arg... trailingArgs) {
        QLFunctionSignature s = signature().returning(returns).arg(receiver).as(args -> args.get(0));
        for (Arg a : trailingArgs) {
            s.arg(a);
        }
        return s;
    }
}
