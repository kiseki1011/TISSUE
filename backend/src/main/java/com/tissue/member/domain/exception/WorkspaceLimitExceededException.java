package com.tissue.member.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class WorkspaceLimitExceededException extends BadRequestException {

	public WorkspaceLimitExceededException(String message, Long memberId) {
		super(message);
		addContext("memberId", memberId);
	}
}
