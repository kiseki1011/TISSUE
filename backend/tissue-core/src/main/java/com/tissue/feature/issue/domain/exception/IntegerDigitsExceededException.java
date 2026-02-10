package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.shared.exception.base.BadRequestException;

public class IntegerDigitsExceededException extends BadRequestException {

    public IntegerDigitsExceededException(Long fieldId, int maxIntegerDigits) {
        super(IssueErrorCode.INTEGER_DIGITS_EXCEEDED);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext("maxIntegerDigits", maxIntegerDigits);
    }
}
