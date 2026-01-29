package com.tissue.issuetype.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_TYPE_ID;

import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.issuetype.domain.IssueType;

public class IssueFieldNotFoundException extends ResourceNotFoundException {

    public IssueFieldNotFoundException(Long issueFieldId, IssueType issueType) {
        super(IssueTypeErrorCode.ISSUE_FIELD_NOT_FOUND);
        addContext(ISSUE_FIELD_ID, issueFieldId);
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
