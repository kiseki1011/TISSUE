package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.in.CreateWorkspaceCommand;
import com.tissue.workspace.application.dto.out.command.WorkspaceCreateResponse;

public interface WorkspaceCreateUseCase {

    WorkspaceCreateResponse create(CreateWorkspaceCommand cmd);
}
