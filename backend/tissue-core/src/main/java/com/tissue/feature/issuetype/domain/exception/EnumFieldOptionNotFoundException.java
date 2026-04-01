package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.FIELD_OPTION_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class EnumFieldOptionNotFoundException extends ResourceNotFoundException {

    public EnumFieldOptionNotFoundException(Long issueFieldId, Long optionId) {
        super(IssueTypeErrorCode.FIELD_OPTION_NOT_FOUND);
        addContext(ISSUE_FIELD_ID, issueFieldId);
        addContext(FIELD_OPTION_ID, optionId);
    }
}
