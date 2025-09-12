package com.tissue.api.common.util;

import com.tissue.api.common.exception.type.InvalidOperationException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainPreconditions {

	/**
	 * Requires non-null.
	 */
	public static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new InvalidOperationException(field + " must not be null");
		}
		return value;
	}

	/**
	 * Requires non-blank.
	 */
	public static String requireNotBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new InvalidOperationException(field + " must not be blank");
		}
		return value;
	}

	public static int requirePositive(int value, String field) {
		if (value <= 0) {
			throw new InvalidOperationException(field + " must be > 0");
		}
		return value;
	}

	public static <T extends Comparable<T>> void requireInRange(T value, T minInclusive, T maxInclusive, String field) {
		requireNonNull(value, field);
		if (value.compareTo(minInclusive) < 0 || value.compareTo(maxInclusive) > 0) {
			throw new InvalidOperationException(
				field + " must be between " + minInclusive + " and " + maxInclusive);
		}
	}
}
