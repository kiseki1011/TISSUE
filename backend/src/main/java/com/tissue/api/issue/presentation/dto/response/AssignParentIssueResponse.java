package com.tissue.api.issue.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.Issue;

import lombok.Builder;

@Builder
public record AssignParentIssueResponse(
	Long issueId,
	String issueKey,

	Long parentIssueId,
	String parentIssueKey,

	LocalDateTime assignedAt
) {
	public static AssignParentIssueResponse from(Issue issue) {
		return AssignParentIssueResponse.builder()
			.issueId(issue.getId())
			.issueKey(issue.getIssueKey())
			.parentIssueId(issue.getParentIssue() != null ? issue.getParentIssue().getId() : null)
			.parentIssueKey(issue.getParentIssue() != null ? issue.getParentIssue().getIssueKey() : null)
			.assignedAt(issue.getLastModifiedDate())
			.build();
	}
}
