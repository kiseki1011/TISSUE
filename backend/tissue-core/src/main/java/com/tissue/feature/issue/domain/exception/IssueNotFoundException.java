package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

    public IssueNotFoundException(String issueKey) {
        super(IssueErrorCode.ISSUE_NOT_FOUND);
        addContext(ISSUE_KEY, issueKey);
    }
}
