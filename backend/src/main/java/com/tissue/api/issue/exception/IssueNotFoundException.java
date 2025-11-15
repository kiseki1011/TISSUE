package com.tissue.api.issue.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

	public IssueNotFoundException(String issueKey, String projectKey, String workspaceKey) {
		super("Issue was not found");
		addContext("issueKey", issueKey);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
