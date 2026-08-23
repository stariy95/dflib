package org.dflib.parquet.read.converter;

import org.apache.parquet.column.Dictionary;
import org.dflib.builder.LongAccum;
import org.dflib.builder.LongHolder;
import org.dflib.builder.ObjectAccum;
import org.dflib.builder.ObjectHolder;
import org.dflib.builder.ValueStore;

/**
 * Reads an unsigned 32-bit int column. As Java has no unsigned primitives, the values are widened to "long".
 */
class UnsignedIntConverter extends StoringPrimitiveConverter<Long> {

    public static UnsignedIntConverter of(boolean accum, int accumCapacity, boolean dictionarySupport, boolean allowsNulls) {
        ValueStore<Long> store = allowsNulls
                ? (accum ? new ObjectAccum<>(accumCapacity) : new ObjectHolder<>())
                : (accum ? new LongAccum(accumCapacity) : new LongHolder());

        return new UnsignedIntConverter(store, dictionarySupport, allowsNulls);
    }

    private Long[] dict;

    protected UnsignedIntConverter(ValueStore<Long> store, boolean dictionarySupport, boolean allowsNulls) {
        super(store, dictionarySupport, allowsNulls);
    }

    @Override
    public void addInt(int value) {
        store.pushLong(Integer.toUnsignedLong(value));
    }

    @Override
    public boolean hasDictionarySupport() {
        // if we are boxing to non-primitives, we might as well use the dictionary
        return allowsNulls && dictionarySupport;
    }

    @Override
    public void setDictionary(Dictionary dictionary) {
        int maxId = dictionary.getMaxId();
        this.dict = new Long[maxId + 1];
        for (int i = 0; i <= maxId; i++) {
            dict[i] = Integer.toUnsignedLong(dictionary.decodeToInt(i));
        }
    }

    @Override
    public void addValueFromDictionary(int dictionaryId) {
        store.push(dict[dictionaryId]);
    }
}
