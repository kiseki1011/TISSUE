package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;

import com.tissue.global.exception.base.BadRequestException;

public class ReviewIncompleteException extends BadRequestException {

    public ReviewIncompleteException(String issueKey, int currentApprovals, int requiredApprovals) {
        super(IssueErrorCode.REVIEW_INCOMPLETE);
        addContext(ISSUE_KEY, issueKey);
        addContext("currentApprovals", currentApprovals);
        addContext("requiredApprovals", requiredApprovals);
    }
}
