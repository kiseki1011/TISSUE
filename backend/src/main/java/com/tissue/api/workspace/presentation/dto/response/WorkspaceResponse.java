package com.tissue.api.workspace.presentation.dto.response;

import com.tissue.api.workspace.domain.model.Workspace;

public record WorkspaceResponse(
	String workspaceCode
) {
	public static WorkspaceResponse from(Workspace workspace) {
		return new WorkspaceResponse(workspace.getKey());
	}
}
