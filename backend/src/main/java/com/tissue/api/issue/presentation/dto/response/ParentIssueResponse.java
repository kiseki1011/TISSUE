package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.Issue;

import lombok.Builder;

@Builder
public record ParentIssueResponse(
	String workspaceCode,
	String issueKey,
	String parentIssueKey
) {
	public static ParentIssueResponse from(Issue issue) {
		return ParentIssueResponse.builder()
			.workspaceCode(issue.getWorkspaceCode())
			.issueKey(issue.getIssueKey())
			.parentIssueKey(
				issue.getParentIssue() != null ? issue.getParentIssue().getIssueKey() : null
			)
			.build();
	}
}
