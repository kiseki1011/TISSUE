package com.tissue.feature.workflow.domain.exception;

import com.tissue.feature.workflow.domain.guard.GuardType;

public class InsufficientApprovalsException extends TransitionGuardException {

    public InsufficientApprovalsException(String issueKey, int currentApprovals, int requiredApprovals) {
        super(
                WorkflowErrorCode.INSUFFICIENT_APPROVALS,
                GuardType.APPROVAL_REQUIRED,
                "Needs %d approval(s) but only %d given.".formatted(requiredApprovals, currentApprovals),
                issueKey);
        addDetail("currentApprovals", currentApprovals);
        addDetail("requiredApprovals", requiredApprovals);
    }
}
