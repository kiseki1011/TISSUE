package com.tissue.api.issue.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;
import com.tissue.api.issue.domain.enums.IssueRelationType;

public class IssueRelationCircularDependencyException extends BadRequestException {

	public IssueRelationCircularDependencyException(
		IssueRelationType relationType,
		String sourceIssueKey,
		String targetIssueKey
	) {
		super("Adding this relation('%s' %s '%s') causes a circular dependency."
			.formatted(sourceIssueKey, relationType.toString(), targetIssueKey));
	}
}
