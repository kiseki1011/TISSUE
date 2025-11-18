package com.tissue.api.workspace.application.port.in;

import com.tissue.api.workspace.adapter.in.web.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.adapter.in.web.dto.response.WorkspaceResponse;

public interface WorkspaceCreateUseCase {
	public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long memberId);
}
