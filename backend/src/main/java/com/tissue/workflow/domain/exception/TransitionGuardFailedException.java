package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.GUARD_TYPE;
import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.REASON;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
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
