package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.FIELD_OPTION_ID;

import com.tissue.feature.issuetype.domain.EnumFieldOption;
import com.tissue.shared.exception.base.BadRequestException;

public class EnumFieldOptionInUseException extends BadRequestException {

    public EnumFieldOptionInUseException(EnumFieldOption option) {
        super(IssueTypeErrorCode.FIELD_OPTION_IN_USE);
        addContext(FIELD_OPTION_ID, option.getId());
    }
}
