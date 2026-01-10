package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD_ID;

import com.tissue.global.exception.base.BadRequestException;

public class DecimalScaleExceededException extends BadRequestException {

    public DecimalScaleExceededException(Long fieldId, int maxFractionDigits) {
        super(IssueErrorCode.DECIMAL_SCALE_EXCEEDED);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext("maxFractionDigits", maxFractionDigits);
    }
}
