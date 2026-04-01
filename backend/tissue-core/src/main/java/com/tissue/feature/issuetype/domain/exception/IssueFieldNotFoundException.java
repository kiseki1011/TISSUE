package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class IssueFieldNotFoundException extends ResourceNotFoundException {

    public IssueFieldNotFoundException(Long issueFieldId) {
        super(IssueTypeErrorCode.ISSUE_FIELD_NOT_FOUND);
        addContext(ISSUE_FIELD_ID, issueFieldId);
    }
}
