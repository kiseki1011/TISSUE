package com.tissue.api.common.util;

import java.time.Instant;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workspace.domain.model.Workspace;

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

	public static Instant requireFutureOrPresent(
		Instant instant
	) {
		if (instant == null) {
			return null;
		}
		if (instant.isBefore(Instant.now())) {
			throw new InvalidOperationException("Date cannot be in the past");
		}

		return instant;
	}

	public static void requireMaxSize(
		int currentSize,
		int maxSize,
		@NonNull String collectionName
	) {
		if (currentSize >= maxSize) {
			throw new InvalidOperationException(
				"Maximum %s limit reached: %d/%d".formatted(collectionName, currentSize, maxSize)
			);
		}
	}

	public static void requireInRange(
		@NonNull Integer value,
		int min,
		int max
	) {
		if (value < min || value > max) {
			throw new InvalidOperationException(
				"Must be between %d and %d, but was %d".formatted(min, max, value)
			);
		}
	}

	public static Integer ensureValidPercentageRange(Integer value) {
		if (value == null) {
			return null;
		}
		if (value < MIN_PERCENTAGE || value > MAX_PERCENTAGE) {
			throw new InvalidOperationException(
				"Percentage must be between %d and %d, but was %d".formatted(MIN_PERCENTAGE, MAX_PERCENTAGE, value)
			);
		}

		return value;
	}

	public static void requireSameWorkspace(
		@NonNull Workspace workspace1,
		@NonNull Workspace workspace2
	) {
		if (!workspace1.equals(workspace2)) {
			throw new InvalidOperationException("Resources must be from the same workspace");
		}
	}
}
