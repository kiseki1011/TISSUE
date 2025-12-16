package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class IssueSelfReferenceException extends BadRequestException {

	public IssueSelfReferenceException(String issueKey) {
		super("Issue self reference is not allowed.");
		// TODO: add workspaceKey
		// addContext("workspaceKey", workspaceKey);
		addContext("issueKey", issueKey);
	}
}
