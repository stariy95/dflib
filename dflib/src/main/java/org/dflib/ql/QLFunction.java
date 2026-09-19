package org.dflib.ql;

import org.dflib.Exp;

/**
 * A marker for a class implementing one QL function as a set of public {@code call} overloads, registered via
 * {@link QLFunctions.Builder#function(String, QLFunction)}. Rules for such a class:
 * <ul>
 * <li>the class and its {@code call} methods must be public, and it must not implement {@code Udf0..UdfN};</li>
 * <li>a {@code call} method returns an {@link Exp}. The parser dispatches operators on the actual type of the
 * returned expression, so a method should return the most specific interface it produces ({@code NumExp<?>},
 * {@code StrExp}, ...) for readability, but nothing depends on the declared return type;</li>
 * <li>a parameter declared as a typed expression ({@code NumExp<?>}, {@code Condition}, {@code Exp<Integer>}, ...)
 * accepts an argument of exactly that type, or an untyped one, such as a bare column reference, which the resolver
 * wraps in the cast to the parameter type ({@code count(a)} is invoked as {@code count(castAsBool(a))}). A
 * parameter declared as {@code Exp<?>} or {@code Exp<T>} accepts an argument of any type as is;</li>
 * <li>a non-{@code Exp} parameter is a constant argument (a literal or a bound parameter) of one of {@code int},
 * {@code long}, {@code double}, {@code boolean}, {@code String}, {@code LocalDate}, {@code LocalTime},
 * {@code LocalDateTime}, {@code OffsetDateTime}, {@code Number} or a type variable bounded by it, or
 * {@code Object} (a constant of any type). An {@code int} or {@code long} parameter accepts an integer constant
 * of any width, a {@code double} parameter accepts any numeric constant;</li>
 * <li>a trailing {@code Exp<?>...} parameter accepts any number of arguments of any type;</li>
 * <li>no two overloads may share an argument shape. Among the overloads matching a call, a fixed arity beats
 * varargs, then the one casting fewer untyped arguments, then the one with fewer arguments passed to
 * {@code Exp<?>} parameters wins. Overloads that would cast the same untyped argument to different types
 * (e.g. {@code year(DateExp)} and {@code year(DateTimeExp)} for {@code year(a)}) make the call ambiguous,
 * and the caller has to cast explicitly;</li>
 * <li>argument problems are reported as {@link IllegalArgumentException} naming the function.</li>
 * </ul>
 *
 * @since 2.0.0
 */
public interface QLFunction {
}
