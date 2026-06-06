package com.tissue.feature.workflow.domain.exception;

import com.tissue.feature.workflow.domain.guard.GuardType;

public class AssigneeRequiredException extends TransitionGuardException {

    public AssigneeRequiredException(String issueKey) {
        super(
                WorkflowErrorCode.ASSIGNEE_REQUIRED,
                GuardType.ASSIGNEE_REQUIRED,
                "An assignee is required before this transition.",
                issueKey);
    }
}
