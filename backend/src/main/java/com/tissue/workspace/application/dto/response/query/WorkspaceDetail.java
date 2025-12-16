package com.tissue.workspace.application.dto.response.query;

import java.time.Instant;

import com.tissue.workspace.domain.Workspace;

import lombok.Builder;

@Builder
public record WorkspaceDetail(
	Long id,
	String key,
	String name,
	String description,
	// int memberCount,
	Long createdBy,
	Instant createdAt,
	Long updatedBy,
	Instant updatedAt
) {
	public static WorkspaceDetail from(Workspace workspace) {
		return WorkspaceDetail.builder()
			.id(workspace.getId())
			.key(workspace.getKey())
			.name(workspace.getName())
			.description(workspace.getDescription())
			.createdBy(workspace.getCreatedBy())
			.createdAt(workspace.getCreatedAt())
			.updatedBy(workspace.getLastModifiedBy())
			.updatedAt(workspace.getLastModifiedAt())
			.build();
	}
}
