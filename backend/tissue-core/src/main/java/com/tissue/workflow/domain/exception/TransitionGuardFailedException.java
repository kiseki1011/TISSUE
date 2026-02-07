package com.tissue.workflow.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.GUARD_TYPE;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.REASON;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.workflow.domain.guard.GuardType;

public class TransitionGuardFailedException extends BadRequestException {

    public TransitionGuardFailedException(GuardType guardType, String reason, String issueKey, String workspaceKey) {
        super(
                WorkflowErrorCode.TRANSITION_GUARD_FAILED,
                "%s evaluation failed. Reason: %s.".formatted(guardType, reason));
        addContext(GUARD_TYPE, guardType);
        addContext(REASON, reason);
        addContext(ISSUE_KEY, issueKey);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
