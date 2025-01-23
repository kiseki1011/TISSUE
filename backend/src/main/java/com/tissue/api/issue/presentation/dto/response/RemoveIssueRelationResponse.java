package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.Issue;

import lombok.Builder;

@Builder
public record RemoveIssueRelationResponse(
	String sourceIssueKey,
	String sourceIssueTitle,

	String targetIssueKey,
	String targetIssueTitle
) {
	public static RemoveIssueRelationResponse from(Issue sourceIssue, Issue targetIssue) {
		return RemoveIssueRelationResponse.builder()
			.sourceIssueKey(sourceIssue.getIssueKey())
			.sourceIssueTitle(sourceIssue.getTitle())
			.targetIssueKey(targetIssue.getIssueKey())
			.targetIssueTitle(targetIssue.getTitle())
			.build();
	}
}
