package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;

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
