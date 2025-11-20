package com.tissue.api.workspace.application.dto.response;

import com.tissue.api.workspace.domain.Workspace;

public record WorkspaceCommandResult(
	String workspaceKey
) {
	public static WorkspaceCommandResult from(Workspace workspace) {
		return new WorkspaceCommandResult(workspace.getKey());
	}
}
