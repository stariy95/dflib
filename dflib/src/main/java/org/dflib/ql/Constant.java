package org.dflib.ql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@code Exp} parameter of a QL function that only accepts a constant expression (a QL literal). A
 * non-{@code Exp} parameter of a {@link QLFunction} is a constant implicitly and needs no annotation.
 *
 * @since 2.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Constant {
}
