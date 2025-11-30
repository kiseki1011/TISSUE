package com.tissue.api.issue.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class IssueRelationAlreadyExistsException extends BadRequestException {

	public IssueRelationAlreadyExistsException(String sourceIssueKey, String targetIssueKey) {
		super("Issue relation already exists between source issue '%s' and target issue '%s'."
			.formatted(sourceIssueKey, targetIssueKey));

		addContext("sourceIssueKey", sourceIssueKey);
		addContext("targetIssueKey", targetIssueKey);
	}
}
