package com.tissue.api.workspace.service.command.create;

import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;

public interface WorkspaceCreateService {
	public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long memberId);
}
