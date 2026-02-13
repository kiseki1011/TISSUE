package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EXPECTED_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.INPUT_VALUE;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_NAME;

import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.exception.base.BadRequestException;
import org.jspecify.annotations.Nullable;

public class CustomFieldTypeMismatchException extends BadRequestException {

    public CustomFieldTypeMismatchException(
            Long fieldId, String fieldName, IssueFieldType expectedType, @Nullable Object inputValue) {
        super(IssueErrorCode.CUSTOM_FIELD_TYPE_MISMATCH);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext(ISSUE_FIELD_NAME, fieldName);
        addContext(EXPECTED_TYPE, expectedType);
        addContext(INPUT_VALUE, inputValue);
    }
}
