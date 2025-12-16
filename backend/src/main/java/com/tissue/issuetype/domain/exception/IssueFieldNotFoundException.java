package com.tissue.issuetype.domain.exception;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class IssueFieldNotFoundException extends ResourceNotFoundException {

	public IssueFieldNotFoundException(Long issueFieldId, Long issueTypeId) {
		super("Issue field was not found");
		addContext("issueFieldId", issueFieldId);
		addContext("issueTypeId", issueTypeId);
	}
}
