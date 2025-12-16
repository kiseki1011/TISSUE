package com.tissue.issuetype.application.dto.response;

import com.tissue.issuetype.domain.IssueType;

public record IssueTypeResponse(
	String workspaceKey,
	Long issueTypeId
) {
	public static IssueTypeResponse from(IssueType issueType) {
		return new IssueTypeResponse(issueType.getWorkspaceKey(), issueType.getId());
	}
}
