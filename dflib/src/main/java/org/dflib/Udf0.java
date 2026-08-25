package org.dflib;

/**
 * A user-defined function that produces an Exp without any arguments.
 *
 * @param <R> type of the produced expression
 *
 * @see Udf1
 * @see Udf2
 * @see Udf3
 * @see UdfN
 *
 * @since 2.0.0
 */
@FunctionalInterface
public interface Udf0<R> {

    /**
     * A convenience "cast" method that allows to inline lambdas as a {@link Udf0}.
     *
     * @since 2.0.0
     */
    static <R> Udf0<R> of(Udf0<R> udf) {
        return udf;
    }

    Exp<R> call();

}
