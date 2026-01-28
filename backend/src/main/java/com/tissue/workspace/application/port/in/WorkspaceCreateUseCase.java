package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.workspace.application.dto.response.command.WorkspaceCreateResponse;

public interface WorkspaceCreateUseCase {

    WorkspaceCreateResponse create(CreateWorkspaceCommand cmd);
}
