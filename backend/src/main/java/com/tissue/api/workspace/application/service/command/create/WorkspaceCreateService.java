package com.tissue.api.workspace.application.service.command.create;

import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;

public interface WorkspaceCreateService {
	public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long memberId);
}
