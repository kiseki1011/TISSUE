package com.tissue.api.member.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.MemberException;

public class DuplicateEmailException extends MemberException {

	private static final String MESSAGE = "The given Email already exists";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateEmailException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DuplicateEmailException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
