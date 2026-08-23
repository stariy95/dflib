package org.dflib.parquet.read.converter;

import org.dflib.builder.ObjectAccum;
import org.dflib.builder.ObjectHolder;
import org.dflib.builder.ValueStore;

/**
 * Reads an unsigned 8-bit int column. As Java has no unsigned primitives, the values are widened to "short".
 */
class UnsignedByteConverter extends StoringPrimitiveConverter<Short> {

    public static UnsignedByteConverter of(boolean accum, int accumCapacity, boolean allowsNulls) {
        ValueStore<Short> store = accum ? new ObjectAccum<>(accumCapacity) : new ObjectHolder<>();
        return new UnsignedByteConverter(store, allowsNulls);
    }

    protected UnsignedByteConverter(ValueStore<Short> store, boolean allowsNulls) {
        super(store, false, allowsNulls);
    }

    @Override
    public void addInt(int value) {
        store.push((short) (value & 0xFF));
    }
}
