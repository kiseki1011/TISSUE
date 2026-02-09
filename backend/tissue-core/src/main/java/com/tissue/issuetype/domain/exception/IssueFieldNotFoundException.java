package com.tissue.issuetype.domain.exception;

import static com.tissue.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.exception.ErrorContextKeys.ISSUE_TYPE_ID;
import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.exception.base.ResourceNotFoundException;
import com.tissue.issuetype.domain.IssueType;

public class IssueFieldNotFoundException extends ResourceNotFoundException {

    public IssueFieldNotFoundException(Long issueFieldId, IssueType issueType) {
        super(IssueTypeErrorCode.ISSUE_FIELD_NOT_FOUND);
        addContext(ISSUE_FIELD_ID, issueFieldId);
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }

    public IssueFieldNotFoundException(String projectKey, Long issueTypeId, Long issueFieldId) {
        super(IssueTypeErrorCode.ISSUE_FIELD_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(ISSUE_TYPE_ID, issueTypeId);
        addContext(ISSUE_FIELD_ID, issueFieldId);
    }
}
