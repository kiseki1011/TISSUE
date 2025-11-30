package com.tissue.api.issue.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;
import com.tissue.api.issue.domain.enums.IssueHierarchy;

public class InvalidParentHierarchyException extends BadRequestException {

	public InvalidParentHierarchyException(
		String parentIssueKey,
		IssueHierarchy parentHierarchy,
		String childIssueKey,
		IssueHierarchy childHierarchy
	) {
		super("Parent issue('%s') hierarchy must be exactly one level above the child issue('%s')"
			.formatted(parentIssueKey, childIssueKey));

		// TODO: add workspaceKey
		// addContext("workspaceKey", workspaceKey);

		addContext("parentIssueKey", parentIssueKey);
		addContext("parentHierarchy", parentHierarchy);
		addContext("childIssueKey", childIssueKey);
		addContext("childHierarchy", childHierarchy);

	}
}
