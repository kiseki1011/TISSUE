package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;

public class ParentRequiredException extends BadRequestException {

	public ParentRequiredException(String issueKey, String currentHierarchy) {
		super("Issues of %s hierarchy must have a parent. Cannot stand alone."
			.formatted(IssueHierarchy.getParentRequired()));

		// TODO: add workspaceKey
		// addContext("workspaceKey", workspaceKey);
		addContext("issueKey", issueKey);
		addContext("currentHierarchy", currentHierarchy);
	}
}
