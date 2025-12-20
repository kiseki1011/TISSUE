package com.tissue.workspace.application.service.finder;

import org.springframework.stereotype.Service;

import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.workspace.domain.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceFinder {

	private final WorkspaceCommandRepository workspaceCommandRepository;

	public Workspace findByKey(String workspaceKey) {
		Workspace workspace = workspaceCommandRepository.findByKey(workspaceKey)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));

		if (workspace.isArchived()) {
			throw new WorkspaceArchivedException(workspaceKey);
		}

		return workspace;
	}
}
