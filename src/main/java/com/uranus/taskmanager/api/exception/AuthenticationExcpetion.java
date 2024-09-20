package com.uranus.taskmanager.api.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class AuthenticationExcpetion extends RuntimeException {

	private final String title;
	private final String message;
	private final HttpStatus httpStatus;

	public AuthenticationExcpetion(String title, String message, HttpStatus httpStatus) {
		this.title = title;
		this.message = message;
		this.httpStatus = httpStatus;
	}

	public AuthenticationExcpetion(String message, String title,
		HttpStatus httpStatus, Throwable cause) {
		super(cause);
		this.title = title;
		this.message = message;
		this.httpStatus = httpStatus;
	}
}
