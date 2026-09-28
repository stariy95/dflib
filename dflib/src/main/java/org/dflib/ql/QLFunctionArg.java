package org.dflib.ql;

import org.dflib.Exp;
import org.dflib.exp.ScalarExp;

/**
 * A description of a function argument or a declared parameter: its type and whether it is (or must be) a constant.
 */
record QLFunctionArg(TypeClassifier type, boolean constant) {

    static QLFunctionArg of(Exp<?> exp) {
        return new QLFunctionArg(TypeClassifier.classify(exp), exp instanceof ScalarExp);
    }

    /**
     * Returns the cost of passing the argument to a declared parameter or {@link TypeClassifier#NO_MATCH}
     * if it can not be passed at all. A constant parameter takes the value of a constant argument as is, so it
     * neither accepts a non-constant argument nor casts an untyped one.
     */
    static int matchCost(QLFunctionArg declared, QLFunctionArg actual) {

        int cost = TypeClassifier.matchCost(declared.type(), actual.type());

        if (declared.constant() && (!actual.constant() || cost == TypeClassifier.COERCION)) {
            return TypeClassifier.NO_MATCH;
        }

        return cost;
    }

    /**
     * Returns true if passing the argument to the declared parameter requires a cast.
     */
    static boolean isCoerced(QLFunctionArg declared, QLFunctionArg actual) {
        return matchCost(declared, actual) == TypeClassifier.COERCION;
    }

    @Override
    public String toString() {
        return constant ? "const " + type : type.toString();
    }
}
