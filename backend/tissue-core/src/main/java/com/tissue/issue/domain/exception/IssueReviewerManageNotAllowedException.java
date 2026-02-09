package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ForbiddenException;
import com.tissue.issue.domain.Issue;

public class IssueReviewerManageNotAllowedException extends ForbiddenException {

    public IssueReviewerManageNotAllowedException(String workspaceKey, String projectKey, String issueKey) {
        super(IssueErrorCode.ISSUE_REVIEWER_MANAGE_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(ISSUE_KEY, issueKey);
    }

    public IssueReviewerManageNotAllowedException(Issue issue) {
        super(IssueErrorCode.ISSUE_REVIEWER_MANAGE_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, issue.getWorkspaceKey());
        addContext(PROJECT_KEY, issue.getProjectKey());
        addContext(ISSUE_KEY, issue.getKey());
    }
}
