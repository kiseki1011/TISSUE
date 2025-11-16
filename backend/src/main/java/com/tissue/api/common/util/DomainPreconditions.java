package com.tissue.api.common.util;

import com.tissue.api.common.exception.domain.InvalidPercentageException;
import com.tissue.api.common.exception.domain.SizeLimitExceededException;
import com.tissue.api.common.exception.domain.ValueOutOfRangeException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DomainPreconditions {

	private final static int MIN_PERCENTAGE = 0;
	private final static int MAX_PERCENTAGE = 100;

	public static String nullToEmpty(String val) {
		return val == null ? "" : val;
	}

	public static void requireMaxSize(
		int currentSize,
		int maxSize,
		String collectionName
	) {
		if (currentSize >= maxSize) {
			throw new SizeLimitExceededException(collectionName, currentSize, maxSize);
		}
	}

	public static void requireInRange(
		@NonNull Integer value,
		int min,
		int max
	) {
		if (value < min || value > max) {
			throw new ValueOutOfRangeException(min, max, value);
		}
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
