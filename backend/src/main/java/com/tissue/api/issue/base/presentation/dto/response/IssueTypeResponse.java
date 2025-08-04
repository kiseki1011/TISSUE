package com.tissue.api.issue.base.presentation.dto.response;

import com.tissue.api.issue.base.domain.model.IssueType;

public record IssueTypeResponse(
	String workspaceCode,
	String issueTypeKey
) {
	// TODO: Use Join Fetch to solve additional query
	public static IssueTypeResponse from(IssueType issueType) {
		return new IssueTypeResponse(issueType.getWorkspaceCode(), issueType.getKey());
	}
}
