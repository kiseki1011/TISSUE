package com.tissue.api.issuetype.exception;

import com.tissue.api.common.exception.type.ResourceNotFoundException;

public class IssueTypeNotFoundException extends ResourceNotFoundException {

	public IssueTypeNotFoundException(Long issueTypeId) {
		super("Issue type was not found");
		addContext("issueTypeId", issueTypeId);
	}
}
