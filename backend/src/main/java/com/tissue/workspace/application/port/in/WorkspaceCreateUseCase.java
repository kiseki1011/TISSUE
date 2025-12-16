package com.tissue.workspace.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.workspace.application.dto.response.WorkspaceCommandResponse;

@Transactional
public interface WorkspaceCreateUseCase {

	WorkspaceCommandResponse create(CreateWorkspaceCommand cmd);
}
