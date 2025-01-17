package com.tissue.api.member.exception;

import com.tissue.api.common.exception.ResourceNotFoundException;

public class MemberNotFoundException extends ResourceNotFoundException {

	private static final String ID_MESSAGE = "Member not found with ID: %d";
	private static final String EMAIL_MESSAGE = "Member not found with email: %s";
	private static final String LOGIN_ID_MESSAGE = "Member not found with login ID: %s";

	public MemberNotFoundException(Long id) {
		super(String.format(ID_MESSAGE, id));
	}

	public MemberNotFoundException(String identifier, boolean isEmail) {
		super(String.format(isEmail ? EMAIL_MESSAGE : LOGIN_ID_MESSAGE, identifier));
	}
}
