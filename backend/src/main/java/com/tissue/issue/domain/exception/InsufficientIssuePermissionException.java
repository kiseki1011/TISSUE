package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.ForbiddenException;
import com.tissue.issue.domain.Issue;

public class InsufficientIssuePermissionException extends ForbiddenException {

    public InsufficientIssuePermissionException(String workspaceKey, String projectKey, String issueKey) {
        super(IssueErrorCode.INSUFFICIENT_ISSUE_PERMISSION);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(ISSUE_KEY, issueKey);
    }

    // TODO: 이렇게 사용해도 괜찮나?
    public InsufficientIssuePermissionException(Issue issue) {
        super(IssueErrorCode.INSUFFICIENT_ISSUE_PERMISSION);
        addContext(WORKSPACE_KEY, issue.getWorkspaceKey());
        addContext(PROJECT_KEY, issue.getProjectKey());
        addContext(ISSUE_KEY, issue.getKey());
    }
}
