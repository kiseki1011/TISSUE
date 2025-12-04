package com.tissue.api.member.domain.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;

public class MemberSignupConflictException extends ResourceConflictException {

	public MemberSignupConflictException(String email, String username, Throwable cause) {
		super("A member with this email, or username may already exist. Please try again.", cause);
		addContext("email", email);
		addContext("username", username);
	}
}
