package com.tissue.api.workspace.application.service.finder;

import org.springframework.stereotype.Service;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.domain.port.out.WorkspaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceFinder {

	private final WorkspaceRepository workspaceRepository;

	public Workspace findWorkspace(String workspaceKey) {
		return workspaceRepository.findByKey(workspaceKey)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
	}

	public Workspace findWorkspaceWithMembers(String workspaceKey) {
		return workspaceRepository.findByKeyWithWorkspaceMembers(workspaceKey)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
	}
}
