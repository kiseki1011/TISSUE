package com.tissue.api.member.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;

public class DuplicateEmailException extends ResourceConflictException {

	public DuplicateEmailException(String email) {
		super("The email '%s' is already in use".formatted(email));
		addContext("email", email);
	}

	public DuplicateEmailException(String email, Throwable cause) {
		super("The email '%s' is already in use".formatted(email), cause);
		addContext("email", email);
	}
}
