package com.tissue.api.workspace.service.command.create;

import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;

public interface WorkspaceCreateService {
	public CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long memberId);
}
