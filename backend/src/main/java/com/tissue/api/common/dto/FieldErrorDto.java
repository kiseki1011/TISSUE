package com.tissue.api.common.dto;

import java.util.List;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public record FieldErrorDto(
	String field,
	String rejectedValue,
	String message
) {

	public static final String NULL_PLACEHOLDER = "null";

	public static List<FieldErrorDto> fromBindingResult(BindingResult bindingResult) {
		return bindingResult.getFieldErrors().stream()
			.map(FieldErrorDto::from)
			.toList();
	}

	private static FieldErrorDto from(FieldError error) {
		String rejectedValue = error.getRejectedValue() != null
			? error.getRejectedValue().toString()
			: NULL_PLACEHOLDER;

		return new FieldErrorDto(error.getField(), rejectedValue, error.getDefaultMessage());
	}
}
