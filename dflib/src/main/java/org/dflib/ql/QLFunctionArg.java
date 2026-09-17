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
     * if it can not be passed at all.
     */
    static int matchCost(QLFunctionArg declared, QLFunctionArg actual) {
        if (declared.constant() && !actual.constant()) {
            return TypeClassifier.NO_MATCH;
        }
        return TypeClassifier.matchCost(declared.type(), actual.type());
    }

    @Override
    public String toString() {
        return constant ? "const " + type : type.toString();
    }
}
