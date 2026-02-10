package com.tissue.feature.issuetype.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;

public class OptionLimitExceededException extends BadRequestException {

    public OptionLimitExceededException(int max, int current) {
        super(IssueTypeErrorCode.OPTION_LIMIT_EXCEEDED);
        addContext("maxOptions", max);
        addContext("currentOptions", current);
    }
}
