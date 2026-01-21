package com.tissue.issuetype.domain.exception;

import com.tissue.global.exception.base.BadRequestException;

public class UnsupportedFieldTypeException extends BadRequestException {

    public UnsupportedFieldTypeException(String fieldType, Object rawValue) {
        super(IssueTypeErrorCode.UNSUPPORTED_FIELD_TYPE);
        addContext("fieldType", fieldType);
        addContext("rawValue", rawValue);
    }
}
