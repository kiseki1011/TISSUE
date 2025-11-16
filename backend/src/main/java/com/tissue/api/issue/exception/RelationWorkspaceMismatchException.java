package com.tissue.api.issue.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class RelationWorkspaceMismatchException extends BadRequestException {

	public RelationWorkspaceMismatchException(
		String sourceIssueWorkspaceKey,
		String sourceIssueKey,
		String targetIssueWorkspaceKey,
		String targetIssueKey
	) {
		super("Cross workspace issue relation is not allowed.");

		addContext("sourceIssueWorkspaceKey", sourceIssueWorkspaceKey);
		addContext("sourceIssueKey", sourceIssueKey);
		addContext("targetIssueWorkspaceKey", targetIssueWorkspaceKey);
		addContext("targetIssueKey", targetIssueKey);
	}
}
