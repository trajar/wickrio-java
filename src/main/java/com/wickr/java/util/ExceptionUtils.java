package com.wickr.java.util;

import java.util.Collection;

public class ExceptionUtils {
    public static <T extends Throwable> boolean isException(final Throwable e, final Collection<Class<? extends T>> clazzes) {
        if (null == e) {
            return false;
        }
        if (clazzes.contains(e.getClass())) {
            return true;
        }
        for (final Class<? extends T> clazz : clazzes) {
            if (clazz.isInstance(e)) {
                return true;
            }
        }
        return isException(e.getCause(), clazzes);
    }

    public static <T extends Throwable> boolean isException(final Throwable e, final Class<T> clazz) {
        if (null == e) {
            return false;
        }
        if (clazz.isInstance(e)) {
            return true;
        }
        return isException(e.getCause(), clazz);
    }

    private ExceptionUtils() {

    }
}
