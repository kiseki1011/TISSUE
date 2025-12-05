package com.tissue.api.issuetype.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class IssueTypeNotFoundException extends ResourceNotFoundException {

	public IssueTypeNotFoundException(Long issueTypeId, String projectKey, String workspaceKey) {
		super("Issue type was not found with id '%d', project key '%s', workspace key '%s'."
			.formatted(issueTypeId, projectKey, workspaceKey));

		addContext("issueTypeId", issueTypeId);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
	}
}
