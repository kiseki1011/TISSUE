package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.common.exception.base.BadRequestException;

public class IntegerDigitsExceededException extends BadRequestException {

    public IntegerDigitsExceededException(Long fieldId, int maxIntegerDigits) {
        super(IssueErrorCode.INTEGER_DIGITS_EXCEEDED);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext("maxIntegerDigits", maxIntegerDigits);
    }
}
