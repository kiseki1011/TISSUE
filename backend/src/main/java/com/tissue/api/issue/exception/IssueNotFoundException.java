package com.tissue.api.issue.exception;

import com.tissue.api.common.exception.type.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

	public IssueNotFoundException(String issueKey, String projectKey, String workspaceKey) {
		super("Issue was not found");
		addContext("issueKey", issueKey);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}

	// TODO: sprintKey? sprintId? 식별에 대한 컨텍스트
	public IssueNotFoundException sprintKey(String sprintKey) {
		addContext("sprintKey", sprintKey);
		return this;
	}
}
