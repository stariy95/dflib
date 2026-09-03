package org.dflib.ql;

import org.dflib.ql.QLFunctionDescriptor.Arg;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import static org.dflib.ql.DescriptorBuilder.descriptor;

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
                builder.function(overload(name, t, t, trailingArgs));
            }
        }

        return builder.function(overload(name, TypeClassifier.OBJECT, TypeClassifier.ANY, trailingArgs));
    }

    private static QLFunctionDescriptor overload(
            String name,
            TypeClassifier receiver,
            TypeClassifier returns,
            Arg... trailingArgs) {

        DescriptorBuilder b = descriptor(name).returning(returns).arg(receiver);
        for (Arg a : trailingArgs) {
            b.arg(a);
        }
        return b.as(args -> args.get(0));
    }
}
