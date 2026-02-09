package com.tissue.issuetype.domain.exception;

import static com.tissue.exception.ErrorContextKeys.FIELD_OPTION_ID;
import static com.tissue.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.exception.ErrorContextKeys.ISSUE_TYPE_ID;
import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.exception.base.ResourceNotFoundException;

public class EnumFieldOptionNotFoundException extends ResourceNotFoundException {

    public EnumFieldOptionNotFoundException(String projectKey, Long issueTypeId, Long issueFieldId, Long optionId) {
        super(IssueTypeErrorCode.FIELD_OPTION_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(ISSUE_TYPE_ID, issueTypeId);
        addContext(ISSUE_FIELD_ID, issueFieldId);
        addContext(FIELD_OPTION_ID, optionId);
    }
}
