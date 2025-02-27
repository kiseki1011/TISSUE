package com.tissue.api.workspace.presentation.dto.response;

import com.tissue.api.workspace.domain.Workspace;

import lombok.Builder;

@Builder
public record UpdateIssueKeyResponse(
	Long workspaceId,
	String workspaceCode,
	String issueKeyPrefix
) {
	public static UpdateIssueKeyResponse from(Workspace workspace) {
		return UpdateIssueKeyResponse.builder()
			.workspaceId(workspace.getId())
			.workspaceCode(workspace.getCode())
			.issueKeyPrefix(workspace.getIssueKeyPrefix())
			.build();
	}
}
