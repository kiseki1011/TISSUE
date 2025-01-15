package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class SelfReferenceNotAllowedException extends IssueException {

	private static final String MESSAGE = "Self reference is not allowed.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public SelfReferenceNotAllowedException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public SelfReferenceNotAllowedException(String message) {
		super(message, HTTP_STATUS);
	}
}
