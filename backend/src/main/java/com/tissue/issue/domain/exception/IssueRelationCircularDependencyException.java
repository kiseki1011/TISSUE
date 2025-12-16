package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueRelationType;

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
