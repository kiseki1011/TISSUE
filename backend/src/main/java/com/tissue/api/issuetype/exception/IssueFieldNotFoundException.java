package com.tissue.api.issuetype.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class IssueFieldNotFoundException extends ResourceNotFoundException {

	public IssueFieldNotFoundException(Long issueFieldId, Long issueTypeId) {
		super("Issue field was not found");
		addContext("issueFieldId", issueFieldId);
		addContext("issueTypeId", issueTypeId);
	}
}
