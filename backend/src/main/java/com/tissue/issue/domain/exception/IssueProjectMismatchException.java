package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class IssueProjectMismatchException extends BadRequestException {

	public IssueProjectMismatchException(String message, String issueKey, String projectKey) {
		super(message);
		addContext("issueKey", issueKey);
		addContext("projectKey", projectKey);
	}

	public IssueProjectMismatchException(String issueKey, String projectKey) {
		super("Issue '%s' must be inside project '%s'.".formatted(issueKey, projectKey));
		addContext("issueKey", issueKey);
		addContext("projectKey", projectKey);
	}
}
