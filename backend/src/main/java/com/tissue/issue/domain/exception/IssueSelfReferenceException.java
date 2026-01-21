package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;

public class IssueSelfReferenceException extends BadRequestException {

    public IssueSelfReferenceException(String workspaceKey, String issueKey) {
        super(IssueErrorCode.ISSUE_SELF_REFERENCE);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
    }
}
