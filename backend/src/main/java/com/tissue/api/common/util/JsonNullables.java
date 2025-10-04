package com.tissue.api.common.util;

import java.util.function.Function;

import org.openapitools.jackson.nullable.JsonNullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonNullables {

	public static <T, R> JsonNullable<R> map(
		JsonNullable<T> source,
		Function<T, R> mapper
	) {
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
