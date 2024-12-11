package com.uranus.taskmanager.api.member.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.MemberException;

public class InvalidMemberPasswordException extends MemberException {

	private static final String MESSAGE = "The given password is invalid";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public InvalidMemberPasswordException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidMemberPasswordException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
