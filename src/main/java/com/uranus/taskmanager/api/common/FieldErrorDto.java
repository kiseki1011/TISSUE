package com.uranus.taskmanager.api.common;

import lombok.Getter;

@Getter
public class FieldErrorDto {
	private final String field;
	private final String rejectedValue;
	private final String message;

	public FieldErrorDto(String field, String rejectedValue, String message) {
		this.field = field;
		this.rejectedValue = rejectedValue;
		this.message = message;
	}
}
