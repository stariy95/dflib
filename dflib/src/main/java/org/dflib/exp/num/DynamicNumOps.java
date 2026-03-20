package org.dflib.exp.num;

import org.dflib.Exp;

final class DynamicNumOps {

    private DynamicNumOps() {
    }

    @SuppressWarnings("unchecked")
    static <T> Unary<T> identity() {
        return (f, e) -> (T)e;
    }

    interface Unary<T> {
        T apply(
                NumericExpFactory factory,
                Exp<? extends Number> exp
        );
    }

    interface Binary<T> {
        T apply(
                NumericExpFactory factory,
                Exp<? extends Number> left,
                Exp<? extends Number> right
        );
    }

    interface Ternary<T> {
        T apply(
                NumericExpFactory factory,
                Exp<? extends Number> one,
                Exp<? extends Number> two,
                Exp<? extends Number> three
        );
    }
}
