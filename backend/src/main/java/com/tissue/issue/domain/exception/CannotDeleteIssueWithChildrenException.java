package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;

import com.tissue.global.exception.base.BadRequestException;

public class CannotDeleteIssueWithChildrenException extends BadRequestException {

    public CannotDeleteIssueWithChildrenException(String issueKey) {
        super(IssueErrorCode.CANNOT_DELETE_ISSUE_WITH_CHILDREN);
        addContext(ISSUE_KEY, issueKey);
    }
}
