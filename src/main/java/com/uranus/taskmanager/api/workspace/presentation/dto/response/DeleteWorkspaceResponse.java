package com.uranus.taskmanager.api.workspace.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

/**
 * Todo
 *  - soft delete을 적용하게 되면 deletedAt 수정 필요
 */
public record DeleteWorkspaceResponse(
	Long id,
	String code,
	LocalDateTime deletedAt
) {
	public static DeleteWorkspaceResponse from(Workspace workspace) {
		return new DeleteWorkspaceResponse(
			workspace.getId(),
			workspace.getCode(),
			LocalDateTime.now()
		);
	}
}
