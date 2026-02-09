package com.tissue.issuetype.domain.exception;

import com.tissue.exception.base.BadRequestException;
import org.jspecify.annotations.Nullable;

public class OptionReorderUnknownIdException extends BadRequestException {

    public OptionReorderUnknownIdException(@Nullable Long unknownId) {
        super(IssueTypeErrorCode.OPTION_REORDER_UNKNOWN_ID);
        addContext("unknownOptionId", unknownId);
    }
}
