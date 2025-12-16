package com.tissue.member.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class MemberHasOwnedWorkspacesException extends BadRequestException {

	public MemberHasOwnedWorkspacesException(String message, Long memberId, String username) {
		super(message);
		addContext("memberId", memberId);
		addContext("username", username);
	}
}
