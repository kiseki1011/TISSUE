package com.tissue.api.issue.collaborator.presentation.dto.response;

import com.tissue.api.issue.base.domain.model.Issue;

public record IssueCollaboratorResponse(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
	public static IssueCollaboratorResponse from(Issue issue, Long memberId) {
		return new IssueCollaboratorResponse(issue.getWorkspaceCode(), issue.getIssueKey(), memberId);
	}
}
