package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

	public static final String MESSAGE = "Issue was not found with issue key '%s' in workspace '%s' and project '%s'.";

	public IssueNotFoundException(String issueKey, String projectKey, String workspaceKey) {
		super(MESSAGE.formatted(issueKey, workspaceKey, projectKey));
		addContext("issueKey", issueKey);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
