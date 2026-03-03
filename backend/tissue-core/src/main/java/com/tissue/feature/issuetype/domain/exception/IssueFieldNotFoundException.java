package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_ID;
import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.exception.base.ResourceNotFoundException;

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
