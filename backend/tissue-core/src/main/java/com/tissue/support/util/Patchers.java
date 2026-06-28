package com.tissue.support.util;

import com.tissue.shared.dto.FieldChange;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.openapitools.jackson.nullable.JsonNullable;

public final class Patchers {

    private Patchers() {}

    public static <T> void apply(JsonNullable<T> jn, Consumer<? super T> set) {
        if (jn == null || !jn.isPresent()) {
            return;
        }
        set.accept(jn.get());
    }

    /**
     * Applies the value and logs the field change (before/after values).
     */
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

    /**
     * Applies the value like {@link #applyWithLog}, but logs only that the field
     * changed, not its before/after values.
     *
     * <p>For a large free-text field like the issue body, keeping both copies in
     * every activity entry is inefficient.
     */
    public static <T> void applyAndMarkChanged(
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
            changes.put(fieldName, new FieldChange(null, null));
        }
    }
}
