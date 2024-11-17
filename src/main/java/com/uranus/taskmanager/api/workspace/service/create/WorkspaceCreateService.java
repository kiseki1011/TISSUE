package com.uranus.taskmanager.api.workspace.service.create;

import com.uranus.taskmanager.api.workspace.presentation.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.WorkspaceCreateResponse;

public interface WorkspaceCreateService {
	public WorkspaceCreateResponse createWorkspace(WorkspaceCreateRequest request, Long memberId);
}
