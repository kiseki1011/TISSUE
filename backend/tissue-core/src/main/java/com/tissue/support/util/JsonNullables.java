package com.tissue.support.util;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;

public final class JsonNullables {

    private JsonNullables() {}

    public static <T> JsonNullable<T> setOrKeep(@Nullable T value) {
        if (value == null) {
            return JsonNullable.undefined();
        }
        return JsonNullable.of(value);
    }

    public static <T, R> JsonNullable<R> map(JsonNullable<T> source, Function<T, R> mapper) {
        if (source == null || !source.isPresent()) {
            return JsonNullable.undefined();
        }

        T value = source.get();
        if (value == null) {
            return JsonNullable.of(null);
        }

        return JsonNullable.of(mapper.apply(value));
    }
}
