package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.shared.exception.base.BadRequestException;

public class UnknownCustomFieldIdException extends BadRequestException {

    public UnknownCustomFieldIdException(Long fieldId) {
        super(IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID);
        addContext(ISSUE_FIELD_ID, fieldId);
    }
}
