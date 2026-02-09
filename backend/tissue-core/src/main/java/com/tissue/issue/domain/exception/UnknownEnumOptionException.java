package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.FIELD_OPTION_ID;
import static com.tissue.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.exception.base.BadRequestException;

public class UnknownEnumOptionException extends BadRequestException {

    public UnknownEnumOptionException(Long fieldId, Long optionId) {
        super(IssueErrorCode.UNKNOWN_ENUM_OPTION);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext(FIELD_OPTION_ID, optionId);
    }
}
