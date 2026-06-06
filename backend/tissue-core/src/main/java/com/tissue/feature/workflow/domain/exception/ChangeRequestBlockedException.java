package com.tissue.feature.workflow.domain.exception;

import com.tissue.feature.workflow.domain.guard.GuardType;

public class ChangeRequestBlockedException extends TransitionGuardException {

    public ChangeRequestBlockedException(String issueKey, int changeRequestedCount) {
        super(
                WorkflowErrorCode.CHANGE_REQUEST_BLOCKED,
                GuardType.APPROVAL_REQUIRED,
                "Transition is blocked: %d reviewer(s) requested changes.".formatted(changeRequestedCount),
                issueKey);
        addDetail("changeRequestedCount", changeRequestedCount);
    }
}
