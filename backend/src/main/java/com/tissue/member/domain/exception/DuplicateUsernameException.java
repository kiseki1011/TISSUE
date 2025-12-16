package com.tissue.member.domain.exception;

import com.tissue.common.exception.base.ResourceConflictException;

public class DuplicateUsernameException extends ResourceConflictException {

	public DuplicateUsernameException(String username) {
		super("The username '%s' is already in use".formatted(username));
		addContext("username", username);
	}

	public DuplicateUsernameException(String username, Throwable cause) {
		super("The username '%s' is already in use".formatted(username), cause);
		addContext("username", username);
	}
}
