package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;

public class IssueSelfReferenceException extends BadRequestException {

    public IssueSelfReferenceException(String workspaceKey, String issueKey) {
        super(IssueErrorCode.ISSUE_SELF_REFERENCE);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
    }
}
