package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.Issue;

public record IssueResponse(
	String workspaceCode,
	String issueKey
) {
	public static IssueResponse from(Issue issue) {
		return new IssueResponse(issue.getWorkspaceCode(), issue.getIssueKey());
	}
}
