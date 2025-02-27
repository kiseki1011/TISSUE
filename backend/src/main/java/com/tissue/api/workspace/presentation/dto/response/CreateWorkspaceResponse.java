package com.tissue.api.workspace.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspace.domain.Workspace;

import lombok.Builder;

@Builder
public record CreateWorkspaceResponse(
	Long id,

	String code,
	String name,
	String description,
	String keyPrefix,

	LocalDateTime createdAt,
	Long createdBy
) {
	public static CreateWorkspaceResponse from(Workspace workspace) {
		return CreateWorkspaceResponse.builder()
			.id(workspace.getId())
			.code(workspace.getCode())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.keyPrefix(workspace.getIssueKeyPrefix())
			.createdAt(workspace.getCreatedDate())
			.createdBy(workspace.getCreatedByMember())
			.build();
	}
}
