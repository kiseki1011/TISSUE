package com.tissue.issuetype.domain.exception;

import com.tissue.exception.base.BadRequestException;

public class OptionReorderSizeMismatchException extends BadRequestException {

    public OptionReorderSizeMismatchException(int expected, int actual) {
        super(IssueTypeErrorCode.OPTION_REORDER_SIZE_MISMATCH);
        addContext("expectedSize", expected);
        addContext("actualSize", actual);
    }
}
