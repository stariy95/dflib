package org.dflib.exp.num;

import org.dflib.Exp;
import org.dflib.NumExp;

/**
 * Casts this expression to a numeric type determined at eval time by scanning actual data.
 * Picks the widest numeric type present in the data (e.g., all ints → Integer, mixed → BigDecimal).
 * Throws {@link IllegalArgumentException} if any non-null value is not a {@link Number}.
 *
 * @since 2.0.0
 */
public class CastAsNumExp extends DynamicNumExp1 implements NumExp<Number> {

    @SuppressWarnings("unchecked")
    public CastAsNumExp(Exp<?> exp) {
        super("castAsNumber", (Exp<Number>) exp, DynamicNumOps.identity());
    }

}
