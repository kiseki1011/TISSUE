package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.issue.domain.Issue;

public class IssueParticipantManageNotAllowedException extends ForbiddenException {

    public IssueParticipantManageNotAllowedException(String workspaceKey, String projectKey, String issueKey) {
        super(IssueErrorCode.ISSUE_PARTICIPANT_MANAGE_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(ISSUE_KEY, issueKey);
    }

    public IssueParticipantManageNotAllowedException(Issue issue) {
        super(IssueErrorCode.ISSUE_PARTICIPANT_MANAGE_NOT_ALLOWED);
        addContext(WORKSPACE_KEY, issue.getWorkspaceKey());
        addContext(PROJECT_KEY, issue.getProjectKey());
        addContext(ISSUE_KEY, issue.getKey());
    }
}
