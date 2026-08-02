package org.dflib.print;

/**
 * Converts individual Series and DataFrame values to their printed form.
 */
class ValuePrinter {

    static String print(Object val) {

        // "Class.toString()" is noisy ("class java.lang.String", "interface java.util.List"), so printing
        // the type name instead. Canonical name is preferred, as it renders arrays as "java.lang.String[]",
        // but it is null for anonymous and local classes
        if (val instanceof Class<?> type) {
            String canonical = type.getCanonicalName();
            return canonical != null ? canonical : type.getName();
        }

        return String.valueOf(val);
    }
}
