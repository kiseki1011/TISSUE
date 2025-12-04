package com.tissue.api.member.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class WorkspaceLimitExceededException extends BadRequestException {

	public WorkspaceLimitExceededException(String message, Long memberId) {
		super(message);
		addContext("memberId", memberId);
	}
}
