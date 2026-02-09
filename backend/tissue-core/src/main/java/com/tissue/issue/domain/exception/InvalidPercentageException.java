package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROVIDED_VALUE;

import com.tissue.exception.base.BadRequestException;

public class InvalidPercentageException extends BadRequestException {

    public InvalidPercentageException(Integer inputValue) {
        super(IssueErrorCode.INVALID_PERCENTAGE_EXCEPTION);
        addContext(PROVIDED_VALUE, inputValue);
    }
}
