package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD;
import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD_ID;
import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE_ID;

import com.tissue.global.exception.base.BadRequestException;

public class CustomFieldRequiredException extends BadRequestException {

    public CustomFieldRequiredException(Long issueTypeId, String issueTypeName, Long fieldId, String fieldName) {
        super(IssueErrorCode.CUSTOM_FIELD_REQUIRED);
        addContext(ISSUE_TYPE_ID, issueTypeId);
        addContext(ISSUE_TYPE, issueTypeName);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext(ISSUE_FIELD, fieldName);
    }
}
