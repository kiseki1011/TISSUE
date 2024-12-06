package com.uranus.taskmanager.api.common.dto;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
	int code,
	HttpStatus status,
	String message,
	T data
) {
	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(HttpStatus.OK, message, data);
	}

	public static <T> ApiResponse<T> okWithNoContent(String message) {
		return new ApiResponse<>(HttpStatus.OK, message, null);
	}

	public static <T> ApiResponse<T> created(String message, T data) {
		return new ApiResponse<>(HttpStatus.CREATED, message, data);
	}

	public static <T> ApiResponse<T> fail(HttpStatus status, String message, T data) {
		return new ApiResponse<>(status, message, data);
	}

	private ApiResponse(HttpStatus status, String message, T data) {
		this(status.value(), status, message, data);
	}
}
