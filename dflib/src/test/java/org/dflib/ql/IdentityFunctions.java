package org.dflib.ql;

import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import static org.dflib.ql.QLFunctionSignature.signature;

/**
 * Test stand-ins for the built-in functions whose return type is the type of their receiver.
 */
class IdentityFunctions {

    private IdentityFunctions() {
    }

    /**
     * Registers a function returning its first argument unchanged, with one overload per receiver type.
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
