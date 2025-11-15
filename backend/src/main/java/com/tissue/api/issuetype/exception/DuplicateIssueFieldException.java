package com.tissue.api.issuetype.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;

public class DuplicateIssueFieldException extends ResourceConflictException {

	public DuplicateIssueFieldException(String issueFieldName, String issueTypeName, Long issueTypeId) {
		super("A field named '%s' already exists in issue type '%s'".formatted(issueFieldName, issueTypeName));
		addContext("issueFieldName", issueFieldName);
		addContext("issueTypeName", issueTypeName);
		addContext("issueTypeId", issueTypeId);
	}
}
