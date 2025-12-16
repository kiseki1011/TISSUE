package com.tissue.issuetype.domain.exception;

import com.tissue.common.exception.base.ResourceConflictException;

public class DuplicateIssueTypeException extends ResourceConflictException {

	public DuplicateIssueTypeException(String name, String projectKey) {
		super("The name for this issue type already exists in this project");
		addContext("name", name);
		addContext("projectKey", projectKey);
	}
}
