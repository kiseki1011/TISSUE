package com.tissue.api.common.exception.domain;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class IssueException extends TissueException {
	public IssueException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public IssueException(String messageCode, Object... args) {
		super(messageCode, args, HttpStatus.BAD_REQUEST);
	}
}
