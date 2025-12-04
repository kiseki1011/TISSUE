package com.tissue.api.workspace.application.service.finder;

import org.springframework.stereotype.Service;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.application.port.out.WorkspaceCommandRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceFinder {

	private final WorkspaceCommandRepository workspaceCommandRepository;

	public Workspace findByKey(String workspaceKey) {
		return workspaceCommandRepository.findByKey(workspaceKey)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
	}
}
