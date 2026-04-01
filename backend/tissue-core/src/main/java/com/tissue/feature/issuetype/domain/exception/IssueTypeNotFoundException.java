package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_ID;
import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class IssueTypeNotFoundException extends ResourceNotFoundException {

    public IssueTypeNotFoundException(String projectKey, Long issueTypeId) {
        super(IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(ISSUE_TYPE_ID, issueTypeId);
    }

    public IssueTypeNotFoundException(Long issueTypeId) {
        super(IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND);
        addContext(ISSUE_TYPE_ID, issueTypeId);
    }
}
