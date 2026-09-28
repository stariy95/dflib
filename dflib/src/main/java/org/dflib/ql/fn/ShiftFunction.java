package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.ql.QLFunction;
import org.dflib.ql.TypeClassifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * QL {@code shift(e, offset)} and {@code shift(e, offset, filler)} functions.
 *
 * @since 2.0.0
 */
public class ShiftFunction implements QLFunction {

    public <N extends Number> NumExp<N> call(NumExp<N> e, int offset) {
        return e.shift(offset);
    }

    public <N extends Number> NumExp<N> call(NumExp<N> e, int offset, N filler) {
        return e.shift(offset, filler);
    }

    public StrExp call(StrExp e, int offset) {
        return e.shift(offset);
    }

    public StrExp call(StrExp e, int offset, String filler) {
        return e.shift(offset, filler);
    }

    public DateExp call(DateExp e, int offset) {
        return e.shift(offset);
    }

    public DateExp call(DateExp e, int offset, LocalDate filler) {
        return e.shift(offset, filler);
    }

    public TimeExp call(TimeExp e, int offset) {
        return e.shift(offset);
    }

    public TimeExp call(TimeExp e, int offset, LocalTime filler) {
        return e.shift(offset, filler);
    }

    public DateTimeExp call(DateTimeExp e, int offset) {
        return e.shift(offset);
    }

    public DateTimeExp call(DateTimeExp e, int offset, LocalDateTime filler) {
        return e.shift(offset, filler);
    }

    public OffsetDateTimeExp call(OffsetDateTimeExp e, int offset) {
        return e.shift(offset);
    }

    public OffsetDateTimeExp call(OffsetDateTimeExp e, int offset, OffsetDateTime filler) {
        return e.shift(offset, filler);
    }

    // Condition does not override shift() covariantly, so the result is a plain Exp<Boolean>
    public Exp<?> call(Condition e, int offset) {
        return e.shift(offset);
    }

    public Exp<?> call(Condition e, int offset, boolean filler) {
        return e.shift(offset, filler);
    }

    public <T> Exp<T> call(Exp<T> e, int offset) {
        return e.shift(offset);
    }

    // Also catches a typed receiver with a filler of another type, so the filler type is checked here
    @SuppressWarnings("unchecked")
    public <T> Exp<T> call(Exp<T> e, int offset, Object filler) {

        TypeClassifier receiverType = TypeClassifier.classify(e);
        TypeClassifier fillerType = filler != null
                ? TypeClassifier.classify(filler.getClass())
                : TypeClassifier.OBJECT;

        if (receiverType.isTyped() && fillerType.isTyped() && receiverType != fillerType) {
            throw new IllegalArgumentException("shift() filler of type " + fillerType
                    + " is not compatible with a " + receiverType + " expression: " + e.toQL());
        }

        return e.shift(offset, (T) filler);
    }
}
