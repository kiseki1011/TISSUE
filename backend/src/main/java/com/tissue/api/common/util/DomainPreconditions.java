package com.tissue.api.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainPreconditions {

	public static String nullToEmpty(String val) {
		return val == null ? "" : val;
	}

	public static <T> T requireNotNull(T val, String field) {
		if (val == null) {
			throw new RuntimeException(field + " must not be null");
		}
		return val;
	}

	public static String requireNotBlank(String val, String field) {
		val = requireNotNull(val, field);
		if (val.isBlank()) {
			throw new RuntimeException(field + " must not be blank");
		}

		return val;
	}

	public static String requireNotEmpty(String val, String field) {
		if (val.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be empty");
		}
		return val;
	}
}
