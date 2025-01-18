package com.tissue.api.common.exception.domain;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class WorkspaceException extends TissueException {
	public WorkspaceException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public WorkspaceException(String messageCode, Object... args) {
		super(messageCode, args, HttpStatus.BAD_REQUEST);
	}
}
