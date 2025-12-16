package com.tissue.common.util;

import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NullSafe {
	public static <T, R> R get(T target, Function<T, R> mapper) {
		return target != null ? mapper.apply(target) : null;
	}
}
