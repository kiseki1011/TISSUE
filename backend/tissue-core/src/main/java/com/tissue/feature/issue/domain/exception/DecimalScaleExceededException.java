package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.shared.exception.base.BadRequestException;

public class DecimalScaleExceededException extends BadRequestException {

    public DecimalScaleExceededException(Long fieldId, int maxFractionDigits) {
        super(IssueErrorCode.DECIMAL_SCALE_EXCEEDED);
        addContext(ISSUE_FIELD_ID, fieldId);
        addContext("maxFractionDigits", maxFractionDigits);
    }
}
