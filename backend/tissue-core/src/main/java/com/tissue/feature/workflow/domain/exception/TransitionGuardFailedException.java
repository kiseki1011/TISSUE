package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.GUARD_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.REASON;

import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.exception.base.BadRequestException;

public class TransitionGuardFailedException extends BadRequestException {

    public TransitionGuardFailedException(GuardType guardType, String reason, String issueKey) {
        super(
                WorkflowErrorCode.TRANSITION_GUARD_FAILED,
                "%s evaluation failed. Reason: %s.".formatted(guardType, reason));
        addContext(GUARD_TYPE, guardType);
        addContext(REASON, reason);
        addContext(ISSUE_KEY, issueKey);
    }
}
