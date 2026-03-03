package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

    public IssueNotFoundException(String workspaceKey, String issueKey) {
        super(IssueErrorCode.ISSUE_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
    }
}
