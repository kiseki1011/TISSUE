package com.tissue.api.member.exception;

import com.tissue.api.common.exception.type.ResourceNotFoundException;

public class MemberNotFoundException extends ResourceNotFoundException {

	private static final String ID_MESSAGE = "Member not found with ID: %d";
	private static final String IDENTIFIER_MESSAGE = "Member not found with login ID or email. identifier: %s";

	public MemberNotFoundException(Long id) {
		super(String.format(ID_MESSAGE, id));
	}

	public MemberNotFoundException(String identifier) {
		super(String.format(IDENTIFIER_MESSAGE, identifier));
	}
}
