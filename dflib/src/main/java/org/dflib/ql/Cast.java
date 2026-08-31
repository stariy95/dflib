package org.dflib.ql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a typed expression parameter of a {@link QLFunction} that coerces - rather than rejects - an argument whose
 * type is only known at eval time. A {@code @Cast StrExp} parameter receives {@code castAsStr()} of such an argument,
 * a {@code @Cast Condition} parameter receives {@code castAsBool()}. An argument of a known but different type is
 * still rejected, exactly as it would be without the annotation.
 * <p>
 * Only {@code StrExp} and {@code Condition} parameters support this, as they are the only expression types with a
 * total cast from an arbitrary expression. Applying it to any other parameter is a registration error.
 *
 * @since 2.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Cast {
}
