package com.tissue.feature.workflow.domain.exception;

import com.tissue.feature.workflow.domain.guard.GuardType;
import java.util.List;

public class UnresolvedChildIssuesException extends TransitionGuardException {

    public UnresolvedChildIssuesException(String issueKey, List<String> unresolvedChildKeys) {
        super(
                WorkflowErrorCode.UNRESOLVED_CHILD_ISSUES,
                GuardType.CHILD_ISSUES_RESOLVE_REQUIRED,
                "This issue has unresolved child issues: %s. Resolve them first.".formatted(unresolvedChildKeys),
                issueKey);
        addDetail("unresolvedChildKeys", unresolvedChildKeys);
    }
}
