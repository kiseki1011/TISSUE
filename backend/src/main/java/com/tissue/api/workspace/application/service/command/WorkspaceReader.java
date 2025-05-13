package com.tissue.api.workspace.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceReader {

	private final WorkspaceRepository workspaceRepository;

	public Workspace findWorkspace(String workspaceCode) {
		return workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceCode));
	}
}
