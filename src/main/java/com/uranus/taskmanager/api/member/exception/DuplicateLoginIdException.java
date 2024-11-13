package com.uranus.taskmanager.api.member.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.MemberException;

public class DuplicateLoginIdException extends MemberException {

	private static final String MESSAGE = "The given Login ID already exists";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateLoginIdException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DuplicateLoginIdException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
