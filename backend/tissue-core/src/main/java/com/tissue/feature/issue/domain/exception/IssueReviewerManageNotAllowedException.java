package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.shared.exception.base.ForbiddenException;

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
