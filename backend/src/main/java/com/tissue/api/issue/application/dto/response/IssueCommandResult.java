package com.tissue.api.issue.application.dto.response;

import com.tissue.api.issue.domain.Issue;

public record IssueCommandResult(
	String workspaceKey,
	String issueKey
) {
	public static IssueCommandResult from(Issue issue) {
		return new IssueCommandResult(issue.getWorkspaceKey(), issue.getKey());
	}
}
