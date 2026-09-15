package org.dflib.ql.antlr4;

import java.util.Collection;

public class PositionalParamSource {

    final Object[] data;
    int idx;

    public PositionalParamSource(Object[] data) {
        this.data = data;
    }

    Object next() {
        return next(Object.class);
    }

    <T> T next(Class<T> type) {
        if(idx >= data.length) {
            throw new IndexOutOfBoundsException("No parameter set for index " + idx);
        }
        Object next = data[idx++];
        if(next == null) {
            return null;
        }
        if(!type.isAssignableFrom(next.getClass())) {
            throw new ClassCastException("Expecting parameter type " + type + ", got " + next.getClass());
        }
        return type.cast(next);
    }

    /**
     * Returns the next parameter as an array, accepting either an array or a collection.
     */
    Object[] nextArray() {
        Object next = next();
        if (next instanceof Collection) {
            return ((Collection<?>) next).toArray();
        } else if (next instanceof Object[]) {
            return (Object[]) next;
        }
        throw new RuntimeException("Expected array or collection parameter");
    }
}
