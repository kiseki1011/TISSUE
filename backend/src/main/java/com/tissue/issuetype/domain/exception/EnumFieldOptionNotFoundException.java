package com.tissue.issuetype.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.FIELD_OPTION_ID;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.issuetype.domain.IssueField;

public class EnumFieldOptionNotFoundException extends ResourceNotFoundException {

    public EnumFieldOptionNotFoundException(Long optionId, IssueField issueField) {
        super(IssueTypeErrorCode.FIELD_OPTION_NOT_FOUND);
        addContext(FIELD_OPTION_ID, optionId);
        addContext(ISSUE_FIELD_ID, issueField.getId());
    }
}
