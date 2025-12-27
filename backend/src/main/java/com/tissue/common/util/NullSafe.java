package com.tissue.common.util;

import java.util.function.Function;

public final class NullSafe {
    private NullSafe() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static <T, R> R get(T target, Function<T, R> mapper) {
        return target != null ? mapper.apply(target) : null;
    }
}
