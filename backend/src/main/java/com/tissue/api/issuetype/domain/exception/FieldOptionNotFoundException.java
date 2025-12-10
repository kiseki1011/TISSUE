package com.tissue.api.issuetype.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class FieldOptionNotFoundException extends ResourceNotFoundException {

	public FieldOptionNotFoundException(Long enumFieldOptionId, Long issueFieldId) {
		super("Option (for ENUM type field) was not found");
		addContext("enumFieldOptionId", enumFieldOptionId);
		addContext("issueFieldId", issueFieldId);
	}
}
