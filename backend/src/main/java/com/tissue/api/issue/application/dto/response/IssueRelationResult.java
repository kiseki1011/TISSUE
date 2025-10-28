package com.tissue.api.issue.application.dto.response;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;

import lombok.Builder;

@Builder
public record IssueRelationResult(
	String workspaceKey,
	String sourceIssueKey,
	String targetIssueKey,
	Long issueRelationId
) {
	public static IssueRelationResult from(Issue sourceIssue, Issue targetIssue, IssueRelation relation) {
		return IssueRelationResult.builder()
			.workspaceKey(sourceIssue.getWorkspaceKey())
			.sourceIssueKey(sourceIssue.getKey())
			.targetIssueKey(targetIssue.getKey())
			.issueRelationId(relation.getId())
			.build();
	}
}
