package org.dflib.ql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a function parameter that only accepts a constant expression, i.e. a QL literal rather than an expression
 * evaluated per row.
 * <p>
 * On a {@code Udf0..UdfN} {@code call} parameter, which is always an {@code Exp}, this is the only way to declare
 * constancy; the function receives the constant expression and reads its value, e.g. with
 * {@link ConstantArgs#constantValue(org.dflib.Exp)}.
 * <p>
 * On a {@link QLFunction} {@code call} parameter it is usually unnecessary: a parameter of a non-{@code Exp} Java
 * type is an implicit constant argument that the caller receives already unwrapped. It is needed only when the
 * constant has to stay an expression - such as a filler whose type must be compared to another argument's, or one
 * whose Java type is a type variable erasing to {@code Object} - in which case it is declared as
 * {@code @Constant Exp<T>}. It can not be applied to a vararg parameter, which declares no individual argument.
 *
 * @since 2.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Constant {
}
