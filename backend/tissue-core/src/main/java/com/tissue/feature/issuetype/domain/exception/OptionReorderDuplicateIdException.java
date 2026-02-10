package com.tissue.feature.issuetype.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;

public class OptionReorderDuplicateIdException extends BadRequestException {

    public OptionReorderDuplicateIdException() {
        super(IssueTypeErrorCode.OPTION_REORDER_DUPLICATE_ID);
    }
}
