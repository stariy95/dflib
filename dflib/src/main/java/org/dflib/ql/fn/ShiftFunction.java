package org.dflib.ql.fn;

import org.dflib.Condition;
import org.dflib.DateExp;
import org.dflib.DateTimeExp;
import org.dflib.Exp;
import org.dflib.NumExp;
import org.dflib.OffsetDateTimeExp;
import org.dflib.StrExp;
import org.dflib.TimeExp;
import org.dflib.ql.Constant;
import org.dflib.ql.ConstantArgs;
import org.dflib.ql.QLFunction;
import org.dflib.ql.QLFunctionDescriptor.TypeClassifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * QL {@code shift(e, offset)} and {@code shift(e, offset, filler)}: shifts the receiver's values forward or backwards
 * by a constant offset, filling the gap with a constant of the receiver's own type (or with nulls when no filler is
 * given).
 * <p>
 * {@code shift} is covariantly overridden on every typed expression interface, so the result is of the receiver's
 * own type and there is one overload per receiver. {@code Condition} is the exception: it declares no {@code shift}
 * of its own, so shifting a boolean expression goes to {@code Exp.shift} and produces a plain {@code Exp<Boolean>}
 * rather than a {@code Condition} - which is why the two boolean overloads declare {@code Exp<?>}, and why
 * {@code shift} of a condition is not usable as an operand of a boolean operator.
 * <p>
 * The last pair of overloads takes a receiver of any type. They are needed because a receiver whose type is only
 * known at eval time - a bare column reference - has no typed overload to resolve to, and {@code shift(a, 2)} on such
 * a column has always been legal QL.
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

    /**
     * Shifting a {@code Condition} produces a plain {@code Exp<Boolean>}: {@code Condition} does not override
     * {@code shift()} covariantly, so the declared return type has to be the untyped one actually produced.
     */
    public Exp<?> call(Condition e, int offset) {
        return e.shift(offset);
    }

    /**
     * @see #call(Condition, int)
     */
    public Exp<?> call(Condition e, int offset, boolean filler) {
        return e.shift(offset, filler);
    }

    /**
     * Shifts a receiver whose type is not known statically, such as a bare column reference.
     */
    public <T> Exp<T> call(Exp<T> e, int offset) {
        return e.shift(offset);
    }

    /**
     * Shifts a receiver whose type is not known statically, filling the gap with a constant.
     * <p>
     * This overload also matches a <i>typed</i> receiver with a filler of another type - {@code shift(int(a), 2,
     * 'replace')} - because the typed overload rejects the filler and this one accepts a receiver of any type at the
     * cost of a wildcard match. The classifier lattice can not express "a filler of the receiver's own type", so this
     * is the one built-in that checks its arguments by hand.
     * <p>
     * The filler is declared as a constant expression rather than as a bare value: an unbounded type variable erases
     * to {@code Object}, which is not one of the Java types an implicit constant parameter may be declared as. Taking
     * it as {@code @Constant Exp<T>} keeps the argument shape identical - a constant of any type - and lets the check
     * below classify the filler by the very same rules the resolver used on it.
     */
    public <T> Exp<T> call(Exp<T> e, int offset, @Constant Exp<T> filler) {

        TypeClassifier receiverType = TypeClassifier.classify(e);
        TypeClassifier fillerType = TypeClassifier.classify(filler);

        if (receiverType.isTyped() && fillerType.isTyped() && receiverType != fillerType) {
            throw new IllegalArgumentException("shift() filler of type " + fillerType
                    + " is not compatible with a " + receiverType + " expression: " + e.toQL());
        }

        return e.shift(offset, ConstantArgs.constantValue(filler));
    }
}
