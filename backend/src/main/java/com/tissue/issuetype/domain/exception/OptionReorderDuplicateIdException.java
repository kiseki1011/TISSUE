package com.tissue.issuetype.domain.exception;

import com.tissue.global.exception.base.BadRequestException;

public class OptionReorderDuplicateIdException extends BadRequestException {

    public OptionReorderDuplicateIdException() {
        super(IssueTypeErrorCode.OPTION_REORDER_DUPLICATE_ID);
    }
}
