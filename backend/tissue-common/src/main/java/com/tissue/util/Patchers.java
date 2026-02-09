package com.tissue.util;

import com.tissue.dto.FieldChange;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.openapitools.jackson.nullable.JsonNullable;

public final class Patchers {

    private Patchers() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static <T> void apply(JsonNullable<T> jn, Consumer<? super T> set) {
        if (jn == null || !jn.isPresent()) {
            return;
        }
        set.accept(jn.get());
    }

    public static <T> void applyWithLog(
            JsonNullable<T> jn,
            Supplier<T> getter,
            Consumer<T> setter,
            String fieldName,
            Map<String, FieldChange> changes) {
        if (jn == null || !jn.isPresent()) {
            return;
        }

        T newValue = jn.get();
        T oldValue = getter.get();

        if (!Objects.equals(oldValue, newValue)) {
            setter.accept(newValue);
            changes.put(fieldName, new FieldChange(oldValue, newValue));
        }
    }
}
