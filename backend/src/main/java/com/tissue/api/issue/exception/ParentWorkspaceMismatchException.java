package com.tissue.api.issue.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class ParentWorkspaceMismatchException extends BadRequestException {

	public ParentWorkspaceMismatchException(
		String parentWorkspaceKey,
		String parentIssueKey,
		String childWorkspaceKey,
		String childIssueKey
	) {
		super("Parent issue(%s:%s) must belong to the same workspace as the child(%s:%s)."
			.formatted(parentWorkspaceKey, parentIssueKey, childWorkspaceKey, childIssueKey));

		addContext("parentWorkspaceKey", parentWorkspaceKey);
		addContext("parentIssueKey", parentIssueKey);
		addContext("childWorkspaceKey", childWorkspaceKey);
		addContext("childIssueKey", childIssueKey);
	}
}
