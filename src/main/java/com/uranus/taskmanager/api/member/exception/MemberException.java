package com.uranus.taskmanager.api.member.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.CommonException;

public abstract class MemberException extends CommonException {

	public MemberException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public MemberException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
