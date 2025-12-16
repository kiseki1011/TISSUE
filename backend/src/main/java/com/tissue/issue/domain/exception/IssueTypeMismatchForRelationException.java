package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issuetype.domain.IssueType;

public class IssueTypeMismatchForRelationException extends BadRequestException {

	public IssueTypeMismatchForRelationException(
		IssueRelationType relationType,
		IssueType sourceType,
		IssueType targetType,
		String workspaceKey,
		String sourceIssueKey,
		String targetIssueKey
	) {
		super("%s relation requires issues to be of the same issue type.".formatted(relationType.toString()));
		addContext("relationType", relationType.toString());
		addContext("sourceIssueType", sourceType.getLabel());
		addContext("targetIssueType", targetType.getLabel());

		addContext("sourceIssueKey", sourceIssueKey);
		addContext("targetIssueKey", targetIssueKey);
		addContext("workspaceKey", workspaceKey);
	}
}
