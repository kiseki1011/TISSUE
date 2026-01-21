package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.EXPECTED_TYPE;
import static com.tissue.global.exception.ContextKeys.INPUT_VALUE;
import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD;
import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD_ID;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import org.jspecify.annotations.Nullable;

public class CustomFieldTypeMismatchException extends BadRequestException {

    public CustomFieldTypeMismatchException(
            Long fieldId, String fieldName, IssueFieldType expectedType, @Nullable Object inputValue) {
        super(IssueErrorCode.CUSTOM_FIELD_TYPE_MISMATCH);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext(ISSUE_FIELD, fieldName);
        addContext(EXPECTED_TYPE, expectedType);
        addContext(INPUT_VALUE, inputValue);
    }
}
