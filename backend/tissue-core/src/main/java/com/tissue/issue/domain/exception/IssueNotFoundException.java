package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.ISSUE_ID;
import static com.tissue.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

    public IssueNotFoundException(String workspaceKey, String issueKey) {
        super(IssueErrorCode.ISSUE_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
    }

    public IssueNotFoundException(Long issueId) {
        super(IssueErrorCode.ISSUE_NOT_FOUND);
        addContext(ISSUE_ID, issueId);
    }
}
