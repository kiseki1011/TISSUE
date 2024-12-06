package com.uranus.taskmanager.api.common.dto;

public record FieldErrorDto(
	String field,
	String rejectedValue,
	String message
) {
}
