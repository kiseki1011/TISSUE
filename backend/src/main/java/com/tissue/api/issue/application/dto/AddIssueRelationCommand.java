package com.tissue.api.issue.application.dto;

import com.tissue.api.issue.domain.enums.IssueRelationType;

import lombok.Builder;

@Builder
public record AddIssueRelationCommand(
	String workspaceKey,
	String sourceIssueKey,
	String targetIssueKey,
	IssueRelationType relationType
) {
}
