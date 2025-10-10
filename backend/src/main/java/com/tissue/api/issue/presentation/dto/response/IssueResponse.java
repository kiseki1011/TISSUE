package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.model.Issue;

public record IssueResponse(
	String workspaceKey,
	String issueKey
) {
	public static IssueResponse from(Issue issue) {
		return new IssueResponse(issue.getWorkspaceKey(), issue.getKey());
	}
}
