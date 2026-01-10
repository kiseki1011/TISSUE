package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;

public class InsufficientIssuePermissionException extends ForbiddenException {

    public InsufficientIssuePermissionException(String workspaceKey, String projectKey, String issueKey) {
        super(IssueErrorCode.INSUFFICIENT_ISSUE_PERMISSION);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(ISSUE_KEY, issueKey);
    }
}
