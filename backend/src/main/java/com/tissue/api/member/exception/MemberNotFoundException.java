package com.tissue.api.member.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.MemberException;

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
