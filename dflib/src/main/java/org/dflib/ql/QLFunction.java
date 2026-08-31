package org.dflib.ql;

import org.dflib.Exp;

/**
 * A marker for a class that implements one QL function as a set of typed {@code call} overloads. Every
 * {@code public call(..)} method <b>declared in the class</b> becomes one signature of the function, reflected into
 * its own {@link QLFunctionDescriptor} when the class is registered via
 * {@link QLFunctions.Builder#function(String, QLFunction)}.
 * <p>
 * Unlike the {@code Udf0..UdfN} registration path, which describes a single lambda, this one lets a function be
 * defined per receiver type and per arity, with javac checking each overload body against the expression interface it
 * declares.
 *
 * <h2>Rules for writing a QLFunction class</h2>
 * <ol>
 * <li><b>The class must be public</b>, and so must every {@code call} method on it, so that the producer can invoke
 * them without {@code setAccessible}. Bridge and synthetic methods are ignored; a class declaring no public
 * {@code call} method is rejected at registration.</li>
 *
 * <li><b>Never also implement {@code Udf0..UdfN}.</b> Their {@code call(String)} / {@code call(int)} default methods
 * are public {@code call} methods and would register as bogus signatures. Such a class is rejected at
 * registration.</li>
 *
 * <li><b>The return type must be the exact {@link Exp} subinterface the body produces</b> - {@code NumExp<?>},
 * {@code StrExp}, {@code DateExp}, {@code Condition}, ... - or {@code Exp<?>} / {@code <T> Exp<T>} when the produced
 * expression implements none of them. The declared return is what the parser records as the call's type and casts on,
 * so a bare {@code Exp<String>} or {@code Exp<Integer>} return - which would claim STRING or NUMERIC while producing
 * an expression that is neither - is rejected at registration.</li>
 *
 * <li><b>A parameter declared as a typed {@code Exp} interface only accepts that type.</b> An argument of another
 * known type does not resolve to this overload at all; an argument whose type is only known at eval time is passed in
 * and checked by the producer. A parameter declared as {@code Exp<?>} accepts anything, and the body must handle
 * anything.</li>
 *
 * <li><b>{@link Cast} on a typed parameter</b> additionally coerces an argument of an unknown type instead of
 * rejecting it: {@code @Cast StrExp} takes {@code castAsStr()} of it, {@code @Cast Condition} takes
 * {@code castAsBool()}. An argument of a known but different type is still rejected. Only these two parameter types
 * support {@code @Cast}, as they are the only ones with a total cast from any expression.</li>
 *
 * <li><b>A non-{@code Exp} parameter is an implicit constant argument</b> of its type's classifier: the caller must
 * pass a literal, and the producer unwraps it to the declared Java type. Allowed types are exactly {@code int},
 * {@code long}, {@code double}, {@code boolean}, {@code String}, {@code LocalDate}, {@code LocalTime},
 * {@code LocalDateTime}, {@code OffsetDateTime}, and a type variable bounded by {@code Number}. Anything else is
 * rejected at registration. When the constant has to stay an expression - because its type must be compared with
 * another argument's, or because its Java type is an unbounded type variable, which erases to {@code Object} and so
 * is not one of the types above - declare it as {@link Constant @Constant} {@code Exp<T>} and read its value with
 * {@link ConstantArgs#constantValue(Exp)}. The argument shape is the same either way.</li>
 *
 * <li><b>Trailing varargs</b> ({@code call(Exp<?>... exps)}) declare an unconstrained tail. Any parameters before it
 * are declared arguments that must be present and match. {@link Constant} and {@link Cast} on the vararg parameter
 * are rejected.</li>
 *
 * <li><b>No two overloads may share an argument shape</b> (the tuple of parameter classifiers and constancy). Such a
 * pair is unresolvable by the parser, and is reported as "already defined" when the registry is built. Note that
 * {@code NumExp<?>} and {@code NumExp<N>} share a shape - by design.</li>
 *
 * <li><b>Report argument problems as {@link IllegalArgumentException} naming the function.</b> The producer unwraps
 * the reflective invocation, and the parser reports it with the position of the call.</li>
 * </ol>
 *
 * @since 2.0.0
 */
public interface QLFunction {
}
