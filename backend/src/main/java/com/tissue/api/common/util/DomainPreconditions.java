package com.tissue.api.common.util;

import com.tissue.api.common.exception.domain.InvalidPercentageException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DomainPreconditions {
	private final static int MIN_PERCENTAGE = 0;
	private final static int MAX_PERCENTAGE = 100;

	public static String nullToEmpty(String val) {
		return val == null ? "" : val;
	}

	public static Integer ensureValidPercentageRange(Integer value) {
		if (value == null) {
			return null;
		}
		if (value < MIN_PERCENTAGE || value > MAX_PERCENTAGE) {
			throw new InvalidPercentageException(MIN_PERCENTAGE, MAX_PERCENTAGE, value);
		}

		return value;
	}
}
