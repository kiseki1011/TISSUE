package com.tissue.api.issuetype.exception;

import com.tissue.api.common.exception.type.BadRequestException;

public class UnsupportedFieldTypeException extends BadRequestException {

	public UnsupportedFieldTypeException(Long issueFieldId, String fieldType, Object rawValue) {
		super("Unsupported issue field type");
		addContext("issueFieldId", issueFieldId);
		addContext("fieldType", fieldType);
		addContext("rawValue", rawValue);
	}
}
