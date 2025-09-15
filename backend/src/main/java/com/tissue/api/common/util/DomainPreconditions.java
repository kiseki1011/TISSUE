package com.tissue.api.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainPreconditions {

	public static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	public static <T> T requireNotNull(T val, String field) {
		if (val == null) {
			throw new RuntimeException(field + " must not be null");
		}

		return val;
	}

	public static String requireNotBlank(String val, String field) {
		String strVal = requireNotNull(val, field);

		if (strVal.isEmpty()) {
			throw new RuntimeException(field + " must not be blank");
		}

		return strVal;
	}
}
