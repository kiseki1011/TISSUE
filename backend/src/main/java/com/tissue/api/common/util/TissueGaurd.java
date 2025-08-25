package com.tissue.api.common.util;

import com.tissue.api.common.exception.type.InvalidOperationException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TissueGaurd {

	public static void forbid(boolean condition, String message) {
		if (condition) {
			throw new InvalidOperationException(message);
		}
	}

	public static void conflict(boolean condition, String message) {
		if (condition) {
			throw new InvalidOperationException(message);
		}
	}
}
