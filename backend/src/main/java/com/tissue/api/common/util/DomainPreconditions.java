package com.tissue.api.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainPreconditions {

	public static String nullToEmpty(String val) {
		return val == null ? "" : val;
	}

	public static String requireNotBlank(String val) {
		if (val.isBlank()) {
			throw new RuntimeException("must not be blank");
		}
		return val;
	}

	public static String requireNotEmpty(String val) {
		if (val.isEmpty()) {
			throw new IllegalArgumentException("must not be empty");
		}
		return val;
	}
}
