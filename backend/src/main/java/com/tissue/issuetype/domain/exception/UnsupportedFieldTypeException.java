package com.tissue.issuetype.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class UnsupportedFieldTypeException extends BadRequestException {

	public UnsupportedFieldTypeException(Long issueFieldId, String fieldType, Object rawValue) {
		super("Unsupported issue field type");
		addContext("issueFieldId", issueFieldId);
		addContext("fieldType", fieldType);
		addContext("rawValue", rawValue);
	}
}
