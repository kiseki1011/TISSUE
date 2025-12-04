package com.tissue.api.workspace.application.dto.response;

import com.tissue.api.workspace.domain.Workspace;

public record WorkspaceCommandResponse(
	String workspaceKey
) {
	public static WorkspaceCommandResponse from(Workspace workspace) {
		return new WorkspaceCommandResponse(workspace.getKey());
	}
}
