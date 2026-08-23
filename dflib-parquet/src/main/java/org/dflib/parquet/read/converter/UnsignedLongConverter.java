package org.dflib.parquet.read.converter;

import org.apache.parquet.column.Dictionary;
import org.dflib.builder.ObjectAccum;
import org.dflib.builder.ObjectHolder;
import org.dflib.builder.ValueStore;

import java.math.BigInteger;

/**
 * Reads an unsigned 64-bit int column. Values above {@link Long#MAX_VALUE} do not fit in any Java primitive,
 * so they are read as BigInteger.
 */
class UnsignedLongConverter extends StoringPrimitiveConverter<BigInteger> {

    public static UnsignedLongConverter of(boolean accum, int accumCapacity, boolean dictionarySupport, boolean allowsNulls) {
        ValueStore<BigInteger> store = accum ? new ObjectAccum<>(accumCapacity) : new ObjectHolder<>();
        return new UnsignedLongConverter(store, dictionarySupport, allowsNulls);
    }

    private BigInteger[] dict;

    protected UnsignedLongConverter(ValueStore<BigInteger> store, boolean dictionarySupport, boolean allowsNulls) {
        super(store, dictionarySupport, allowsNulls);
    }

    @Override
    public void addLong(long value) {
        store.push(convert(value));
    }

    @Override
    public void setDictionary(Dictionary dictionary) {
        int maxId = dictionary.getMaxId();
        this.dict = new BigInteger[maxId + 1];
        for (int i = 0; i <= maxId; i++) {
            dict[i] = convert(dictionary.decodeToLong(i));
        }
    }

    @Override
    public void addValueFromDictionary(int dictionaryId) {
        store.push(dict[dictionaryId]);
    }

    private BigInteger convert(long value) {
        return value >= 0 ? BigInteger.valueOf(value) : new BigInteger(Long.toUnsignedString(value));
    }
}
