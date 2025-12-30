package com.tissue.common.util;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public final class NullSafe {

    private NullSafe() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    // TODO: refactor to not use this util. make a getParentId, getParentKey in the Issue entity.
    public static <T, R> @Nullable R get(@Nullable T target, Function<T, R> mapper) {
        return target != null ? mapper.apply(target) : null;
    }
}
