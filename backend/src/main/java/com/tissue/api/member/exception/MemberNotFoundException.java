package com.tissue.api.member.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class MemberNotFoundException extends ResourceNotFoundException {

	public MemberNotFoundException(Long memberId) {
		super(String.format("Could not find member with id '%d'", memberId));
	}

	public MemberNotFoundException(String email) {
		super(String.format("Could not find member with email '%s'", email));
	}
}
