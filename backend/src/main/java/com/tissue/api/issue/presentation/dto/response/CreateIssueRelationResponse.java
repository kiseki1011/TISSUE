package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueRelationType;

import lombok.Builder;

@Builder
public record CreateIssueRelationResponse(

	String sourceIssueKey,
	String sourceIssueTitle,
	String targetIssueKey,
	String targetIssueTitle,
	IssueRelationType relationType,
	IssueRelationType oppositeRelationType

) {

	public static CreateIssueRelationResponse from(
		Issue sourceIssue,
		Issue targetIssue,
		IssueRelationType relationType
	) {
		return CreateIssueRelationResponse.builder()
			.sourceIssueKey(sourceIssue.getIssueKey())
			.sourceIssueTitle(sourceIssue.getTitle())
			.targetIssueKey(targetIssue.getIssueKey())
			.targetIssueTitle(targetIssue.getTitle())
			.relationType(relationType)
			.oppositeRelationType(relationType.getOpposite())
			.build();
	}
}
