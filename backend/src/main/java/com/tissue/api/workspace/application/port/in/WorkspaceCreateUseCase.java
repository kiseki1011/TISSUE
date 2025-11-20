package com.tissue.api.workspace.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;

public interface WorkspaceCreateUseCase {

	@Transactional
	WorkspaceCommandResult create(CreateWorkspaceCommand cmd);
}
