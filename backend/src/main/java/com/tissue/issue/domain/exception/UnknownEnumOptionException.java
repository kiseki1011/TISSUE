package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.FIELD_OPTION_ID;
import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD_ID;

import com.tissue.global.exception.base.BadRequestException;

public class UnknownEnumOptionException extends BadRequestException {

    public UnknownEnumOptionException(Long fieldId, Long optionId) {
        super(IssueErrorCode.UNKNOWN_ENUM_OPTION);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext(FIELD_OPTION_ID, optionId);
    }
}
