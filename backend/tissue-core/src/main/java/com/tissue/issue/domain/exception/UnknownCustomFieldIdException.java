package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.common.exception.base.BadRequestException;

public class UnknownCustomFieldIdException extends BadRequestException {

    public UnknownCustomFieldIdException(Long fieldId) {
        super(IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID);
        addContext(ISSUE_FIELD_ID, fieldId);
    }
}
