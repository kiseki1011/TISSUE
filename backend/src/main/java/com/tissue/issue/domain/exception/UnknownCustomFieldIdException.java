package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD_ID;

import com.tissue.global.exception.base.BadRequestException;

public class UnknownCustomFieldIdException extends BadRequestException {

    public UnknownCustomFieldIdException(Long fieldId) {
        super(IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID);
        addContext(ISSUE_FIELD_ID, fieldId);
    }
}
