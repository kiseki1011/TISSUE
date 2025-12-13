package com.tissue.api.common.util;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.dto.FieldChange;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Patchers {

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
		Map<String, FieldChange> changes
	) {
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
