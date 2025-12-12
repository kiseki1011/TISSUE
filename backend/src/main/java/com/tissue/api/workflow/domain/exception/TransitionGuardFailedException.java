package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;
import com.tissue.api.workflow.domain.guard.GuardType;

public class TransitionGuardFailedException extends BadRequestException {

	public TransitionGuardFailedException(GuardType guardType, String reason, String issueKey, String workspaceKey) {
		super("[%s] Guard failed: %s".formatted(guardType, reason));

		addContext("failedGuardType", guardType);
		addContext("failureReason", reason);
		addContext("issueKey", issueKey);
		addContext("workspaceKey", workspaceKey);
	}
}
