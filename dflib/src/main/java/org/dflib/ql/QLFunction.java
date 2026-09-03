package org.dflib.ql;

import org.dflib.Exp;

/**
 * A marker for a class implementing one QL function as a set of public {@code call} overloads, registered via
 * {@link QLFunctions.Builder#function(String, QLFunction)}. Rules for such a class:
 * <ul>
 * <li>the class and its {@code call} methods must be public, and it must not implement {@code Udf0..UdfN};</li>
 * <li>a {@code call} method returns the exact {@link Exp} subinterface it produces ({@code NumExp<?>},
 * {@code StrExp}, ...), or {@code Exp<?>} if it produces none of them;</li>
 * <li>a typed {@code Exp} parameter accepts an argument of that type, or one whose type is only known at eval time;
 * an {@code Exp<?>} parameter accepts anything;</li>
 * <li>a non-{@code Exp} parameter is a constant argument of one of {@code int}, {@code long}, {@code double},
 * {@code boolean}, {@code String}, {@code LocalDate}, {@code LocalTime}, {@code LocalDateTime},
 * {@code OffsetDateTime}, {@code Object} (a constant of any type) or a type variable bounded by {@code Number};</li>
 * <li>a trailing {@code Exp<?>...} parameter accepts any number of arguments of any type;</li>
 * <li>no two overloads may share an argument shape;</li>
 * <li>argument problems are reported as {@link IllegalArgumentException} naming the function.</li>
 * </ul>
 *
 * @since 2.0.0
 */
public interface QLFunction {
}
