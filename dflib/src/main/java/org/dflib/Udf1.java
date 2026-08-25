package org.dflib;


import static org.dflib.Exp.$col;

/**
 * A user-defined function that produces an {@link Exp} based on a single columnar argument. The argument can be either
 * itself an expression or a DataFrame column reference.
 */
@FunctionalInterface
public interface Udf1<A, R> {

    /**
     * A convenience "cast" method that allows to inline lambdas as a {@link Udf1}.
     *
     * @since 2.0.0
     */
    static <A, R> Udf1<A, R> of(Udf1<A, R> udf) {
        return udf;
    }

    /**
     * Produces an expression based on the provided expression. This should be the preferred (though also most verbose)
     * way to resolve a UDF if the type of column is of significance. I.e. <code>call($int("a"))</code> will be faster
     * than <code>call("a")</code>, if the UDF converts the contents of "a" to ints internally.
     */
    Exp<R> call(Exp<A> exp);

    default Exp<R> call(String column) {
        return call($col(column));
    }

    default Exp<R> call(int columnPos) {
        return call($col(columnPos));
    }
}
