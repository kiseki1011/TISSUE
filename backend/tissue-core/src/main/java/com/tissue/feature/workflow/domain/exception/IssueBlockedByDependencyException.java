package com.tissue.feature.workflow.domain.exception;

import com.tissue.feature.workflow.domain.guard.GuardType;
import java.util.List;

public class IssueBlockedByDependencyException extends TransitionGuardException {

    public IssueBlockedByDependencyException(String issueKey, List<String> blockingIssueKeys) {
        super(
                WorkflowErrorCode.TRANSITION_BLOCKED_BY_DEPENDENCY,
                GuardType.BLOCKING_ISSUE_RESOLVE_REQUIRED,
                "This issue is blocked by unresolved issues: %s. Resolve them first.".formatted(blockingIssueKeys),
                issueKey);
        addDetail("blockingIssueKeys", blockingIssueKeys);
    }
}
