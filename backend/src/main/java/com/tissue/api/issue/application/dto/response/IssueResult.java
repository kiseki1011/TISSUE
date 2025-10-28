package com.tissue.api.issue.application.dto.response;

import com.tissue.api.issue.domain.Issue;

public record IssueResult(
	String workspaceKey,
	String issueKey
) {
	public static IssueResult from(Issue issue) {
		return new IssueResult(issue.getWorkspaceKey(), issue.getKey());
	}
}
