package com.wickr.java.util;

public class StringUtils {
    public static String join(final String delim, final Iterable<?> elements) {
        final StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (final Object obj : elements) {
            if (obj != null) {
                if (!first) {
                    buffer.append(delim);
                }
                buffer.append(obj);
                first = false;
            }
        }
        return buffer.toString();
    }

    public static String safeNull(final Object obj) {
        if (null == obj) {
            return "";
        }
        return obj.toString();
    }

    private StringUtils() {

    }
}
