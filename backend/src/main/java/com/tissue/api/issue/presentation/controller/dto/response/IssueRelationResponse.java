package com.tissue.api.issue.presentation.controller.dto.response;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;

import lombok.Builder;

@Builder
public record IssueRelationResponse(
	String workspaceCode,
	String sourceIssueKey,
	String targetIssueKey,
	Long issueRelationId
) {
	public static IssueRelationResponse from(Issue sourceIssue, Issue targetIssue, IssueRelation relation) {
		return IssueRelationResponse.builder()
			.workspaceCode(sourceIssue.getWorkspaceCode())
			.sourceIssueKey(sourceIssue.getIssueKey())
			.targetIssueKey(targetIssue.getIssueKey())
			.issueRelationId(relation.getId())
			.build();
	}
}
