package com.uranus.taskmanager.api.member.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.MemberException;

public class MemberNotFoundException extends MemberException {

	private static final String MESSAGE = "Member was not found";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public MemberNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public MemberNotFoundException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
