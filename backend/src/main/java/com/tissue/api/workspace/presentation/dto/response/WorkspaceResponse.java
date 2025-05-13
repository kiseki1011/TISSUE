package com.tissue.api.workspace.presentation.dto.response;

import com.tissue.api.workspace.domain.Workspace;

public record WorkspaceResponse(
	String workspaceCode
) {
	public static WorkspaceResponse from(Workspace workspace) {
		return new WorkspaceResponse(workspace.getCode());
	}
}
