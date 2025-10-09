package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.model.Issue;

public record IssueCollaboratorResponse(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
	public static IssueCollaboratorResponse from(Issue issue, Long memberId) {
		return new IssueCollaboratorResponse(issue.getWorkspaceKey(), issue.getKey(), memberId);
	}
}
