package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class TissueException extends RuntimeException {

	private final HttpStatus httpStatus;
	private final String messageCode;
	private final transient Object[] args;

	protected TissueException(String messageCode, Object[] args, HttpStatus status) {
		this.messageCode = messageCode;
		this.args = args;
		this.httpStatus = status;
	}

	protected TissueException(String message, HttpStatus status) {
		super(message);
		this.httpStatus = status;
		this.messageCode = null;
		this.args = null;
	}

	public boolean hasMessageCode() {
		return messageCode != null;
	}
}
