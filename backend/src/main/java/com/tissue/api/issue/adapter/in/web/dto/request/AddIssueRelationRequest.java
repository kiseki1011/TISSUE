package com.tissue.api.issue.adapter.in.web.dto.request;

import com.tissue.api.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.api.issue.domain.enums.IssueRelationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddIssueRelationRequest(
	@NotBlank String targetIssueKey,
	@NotNull IssueRelationType relationType
) {
	public AddIssueRelationCommand toCommand(String workspaceKey, String sourceIssueKey) {
		return AddIssueRelationCommand.builder()
			.workspaceKey(workspaceKey)
			.sourceIssueKey(sourceIssueKey)
			.targetIssueKey(targetIssueKey)
			.relationType(relationType)
			.build();
	}
}
