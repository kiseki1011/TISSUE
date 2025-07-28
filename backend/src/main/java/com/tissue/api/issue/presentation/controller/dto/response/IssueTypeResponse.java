package com.tissue.api.issue.presentation.controller.dto.response;

import com.tissue.api.issue.domain.newmodel.IssueTypeDefinition;

public record IssueTypeResponse(
	String workspaceCode,
	String issueTypeKey
) {
	// TODO: Use Join Fetch to solve additional query
	public static IssueTypeResponse from(IssueTypeDefinition issueType) {
		return new IssueTypeResponse(issueType.getWorkspaceCode(), issueType.getKey());
	}
}
