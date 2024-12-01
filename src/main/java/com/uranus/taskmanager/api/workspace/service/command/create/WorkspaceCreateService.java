package com.uranus.taskmanager.api.workspace.service.command.create;

import com.uranus.taskmanager.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.CreateWorkspaceResponse;

public interface WorkspaceCreateService {
	public CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long memberId);
}
