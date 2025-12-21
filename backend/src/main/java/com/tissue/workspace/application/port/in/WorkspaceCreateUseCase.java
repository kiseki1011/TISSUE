package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.workspace.application.dto.response.WorkspaceCommandResponse;

public interface WorkspaceCreateUseCase {

	WorkspaceCommandResponse create(CreateWorkspaceCommand cmd);
}
