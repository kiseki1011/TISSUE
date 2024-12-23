package com.tissue.api.common.dto;

public record FieldErrorDto(
	String field,
	String rejectedValue,
	String message
) {
}
