package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.PROVIDED_VALUE;

import com.tissue.common.exception.base.BadRequestException;

public class InvalidPercentageException extends BadRequestException {

    public InvalidPercentageException(Integer inputValue) {
        super(IssueErrorCode.INVALID_PERCENTAGE_EXCEPTION);
        addContext(PROVIDED_VALUE, inputValue);
    }
}
