package com.tissue.api.issue.base.presentation.dto.response;

import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueRelation;

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
			.workspaceCode(sourceIssue.getWorkspaceKey())
			.sourceIssueKey(sourceIssue.getKey())
			.targetIssueKey(targetIssue.getKey())
			.issueRelationId(relation.getId())
			.build();
	}
}
