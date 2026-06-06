package com.tissue.feature.workflow.domain.exception;

import com.tissue.feature.workflow.domain.guard.GuardType;

public class LinkedBranchRequiredException extends TransitionGuardException {

    public LinkedBranchRequiredException(String issueKey) {
        super(
                WorkflowErrorCode.LINKED_BRANCH_REQUIRED,
                GuardType.LINKED_BRANCH_REQUIRED,
                "At least one linked VCS branch is required before this transition.",
                issueKey);
    }
}
