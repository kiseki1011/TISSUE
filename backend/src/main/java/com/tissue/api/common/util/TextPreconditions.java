package com.tissue.api.common.util;

import java.util.List;

import com.tissue.api.common.dto.FieldErrorDto;
import com.tissue.api.common.exception.type.FieldValidationException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// TODO: Refactor after refactoring TissueException and implementing ApiCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextPreconditions {

	/**
	 * Requires non-null.
	 */
	public static void requireNonNull(Object value, String field) {
		if (value == null) {
			throw new FieldValidationException(field + " must not be null",
				List.of(new FieldErrorDto(field, null, "Requires not null")));
		}
	}

	/**
	 * Requires non-blank.
	 */
	public static void requireNonBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new FieldValidationException(field + " must not be blank",
				List.of(new FieldErrorDto(field, null, "Requires not blank")));
		}
	}

	/**
	 * Requires length <= max.
	 */
	// public static void requireMaxLength(String value, int max, String field) {
	// 	if (value == null) {
	// 		throw new FieldValidationException(field + " must not be null");
	// 	}
	// 	if (value.length() > max) {
	// 		throw new FieldValidationException(field + " length must be <= " + max);
	// 	}
	// }
}
