package com.tissue.issuetype.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class OptionReorderDuplicateIdException extends BadRequestException {

    public OptionReorderDuplicateIdException() {
        super(IssueTypeErrorCode.OPTION_REORDER_DUPLICATE_ID);
    }
}
