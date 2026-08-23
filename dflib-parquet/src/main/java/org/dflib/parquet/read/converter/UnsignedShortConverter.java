package org.dflib.parquet.read.converter;

import org.dflib.builder.IntAccum;
import org.dflib.builder.IntHolder;
import org.dflib.builder.ObjectAccum;
import org.dflib.builder.ObjectHolder;
import org.dflib.builder.ValueStore;

/**
 * Reads an unsigned 16-bit int column. As Java has no unsigned primitives, the values are widened to "int".
 */
class UnsignedShortConverter extends StoringPrimitiveConverter<Integer> {

    public static UnsignedShortConverter of(boolean accum, int accumCapacity, boolean allowsNulls) {
        ValueStore<Integer> store = allowsNulls
                ? (accum ? new ObjectAccum<>(accumCapacity) : new ObjectHolder<>())
                : (accum ? new IntAccum(accumCapacity) : new IntHolder());

        return new UnsignedShortConverter(store, allowsNulls);
    }

    protected UnsignedShortConverter(ValueStore<Integer> store, boolean allowsNulls) {
        super(store, false, allowsNulls);
    }

    @Override
    public void addInt(int value) {
        store.pushInt(value & 0xFFFF);
    }
}
