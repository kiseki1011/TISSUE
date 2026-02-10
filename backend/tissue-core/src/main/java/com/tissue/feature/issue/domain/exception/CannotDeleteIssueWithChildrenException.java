package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.shared.exception.base.BadRequestException;

public class CannotDeleteIssueWithChildrenException extends BadRequestException {

    public CannotDeleteIssueWithChildrenException(String issueKey) {
        super(IssueErrorCode.CANNOT_DELETE_ISSUE_WITH_CHILDREN);
        addContext(ISSUE_KEY, issueKey);
    }
}
