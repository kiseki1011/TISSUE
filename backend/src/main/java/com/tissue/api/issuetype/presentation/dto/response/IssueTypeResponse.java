package com.tissue.api.issuetype.presentation.dto.response;

import com.tissue.api.issuetype.domain.IssueType;

public record IssueTypeResponse(
	String workspaceKey,
	Long issueTypeId
) {
	public static IssueTypeResponse from(IssueType issueType) {
		return new IssueTypeResponse(issueType.getWorkspaceKey(), issueType.getId());
	}
}
