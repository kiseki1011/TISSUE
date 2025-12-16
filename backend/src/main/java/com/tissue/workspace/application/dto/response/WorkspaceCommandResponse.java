package com.tissue.workspace.application.dto.response;

import com.tissue.workspace.domain.Workspace;

public record WorkspaceCommandResponse(
	String workspaceKey
) {
	public static WorkspaceCommandResponse from(Workspace workspace) {
		return new WorkspaceCommandResponse(workspace.getKey());
	}
}
