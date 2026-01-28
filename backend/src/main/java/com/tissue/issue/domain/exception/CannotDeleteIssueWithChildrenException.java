package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.common.exception.base.BadRequestException;

public class CannotDeleteIssueWithChildrenException extends BadRequestException {

    public CannotDeleteIssueWithChildrenException(String issueKey) {
        super(IssueErrorCode.CANNOT_DELETE_ISSUE_WITH_CHILDREN);
        addContext(ISSUE_KEY, issueKey);
    }
}
