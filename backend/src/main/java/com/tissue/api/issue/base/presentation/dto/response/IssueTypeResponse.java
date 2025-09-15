package com.tissue.api.issue.base.presentation.dto.response;

import com.tissue.api.issue.base.domain.model.IssueType;

public record IssueTypeResponse(
	String workspaceCode,
	Long issueTypeId
) {
	public static IssueTypeResponse from(IssueType issueType) {
		return new IssueTypeResponse(issueType.getWorkspaceCode(), issueType.getId());
	}
}
