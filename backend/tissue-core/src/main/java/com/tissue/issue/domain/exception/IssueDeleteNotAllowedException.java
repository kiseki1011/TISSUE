package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.exception.base.ForbiddenException;

public class IssueDeleteNotAllowedException extends ForbiddenException {

    public IssueDeleteNotAllowedException(String issueKey) {
        super(IssueErrorCode.ISSUE_DELETE_NOT_ALLOWED);
        addContext(ISSUE_KEY, issueKey);
    }
}
