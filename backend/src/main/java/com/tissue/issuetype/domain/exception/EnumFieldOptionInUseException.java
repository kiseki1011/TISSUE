package com.tissue.issuetype.domain.exception;

import static com.tissue.global.exception.ContextKeys.FIELD_OPTION_ID;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issuetype.domain.EnumFieldOption;

public class EnumFieldOptionInUseException extends BadRequestException {

    public EnumFieldOptionInUseException(EnumFieldOption option) {
        super(IssueTypeErrorCode.FIELD_OPTION_IN_USE);
        addContext(FIELD_OPTION_ID, option.getId());
    }
}
