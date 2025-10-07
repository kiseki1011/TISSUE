package com.tissue.api.common.util;

import java.util.function.Consumer;

import org.openapitools.jackson.nullable.JsonNullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Patchers {

	public static <T> void apply(JsonNullable<T> jn, Consumer<? super T> set) {
		if (jn == null || !jn.isPresent()) {
			return; // undefined → 무시
		}
		set.accept(jn.get()); // null/value 모두 위임
	}
}
