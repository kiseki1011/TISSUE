package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueRelation;

import lombok.Builder;

@Builder
public record IssueRelationResponse(
	String workspaceKey,
	String sourceIssueKey,
	String targetIssueKey,
	Long issueRelationId
) {
	public static IssueRelationResponse from(Issue sourceIssue, Issue targetIssue, IssueRelation relation) {
		return IssueRelationResponse.builder()
			.workspaceKey(sourceIssue.getWorkspaceKey())
			.sourceIssueKey(sourceIssue.getKey())
			.targetIssueKey(targetIssue.getKey())
			.issueRelationId(relation.getId())
			.build();
	}
}
