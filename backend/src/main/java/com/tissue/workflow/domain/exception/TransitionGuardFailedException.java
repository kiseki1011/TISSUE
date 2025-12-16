package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.workflow.domain.guard.GuardType;

public class TransitionGuardFailedException extends BadRequestException {

	public TransitionGuardFailedException(GuardType guardType, String reason, String issueKey, String workspaceKey) {
		super("%s evaluation failed: %s".formatted(guardType, reason));

		addContext("failedGuardType", guardType);
		addContext("failureReason", reason);
		addContext("issueKey", issueKey);
		addContext("workspaceKey", workspaceKey);
	}
}
