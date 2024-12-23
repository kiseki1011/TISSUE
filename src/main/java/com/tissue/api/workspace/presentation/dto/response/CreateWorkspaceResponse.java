package com.tissue.api.workspace.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspace.domain.Workspace;

public record CreateWorkspaceResponse(
	Long id,
	String code,
	LocalDateTime createdAt
) {
	public static CreateWorkspaceResponse from(Workspace workspace) {
		return new CreateWorkspaceResponse(
			workspace.getId(),
			workspace.getCode(),
			workspace.getCreatedDate()
		);
	}
}
