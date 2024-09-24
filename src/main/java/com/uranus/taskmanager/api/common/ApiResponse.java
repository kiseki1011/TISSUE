package com.uranus.taskmanager.api.common;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

	private final int code;
	private final HttpStatus status;
	private final String message;
	private final T data;

	public ApiResponse(HttpStatus status, String message, T data) {
		this.code = status.value();
		this.status = status;
		this.message = message;
		this.data = data;
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(HttpStatus.OK, message, data);
	}

	public static <T> ApiResponse<T> okWithNoContent(String message) {
		return new ApiResponse<>(HttpStatus.NO_CONTENT, message, null);
	}

	public static <T> ApiResponse<T> created(String message, T data) {
		return new ApiResponse<>(HttpStatus.CREATED, message, data);
	}

	public static <T> ApiResponse<T> fail(HttpStatus status, String message, T data) {
		return new ApiResponse<>(status, message, data);
	}
}
