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

	// TODO: add javadoc for the following information
	//  - its only for command API's
	//  - will throw an exception if workspace was archived
	public Workspace getModifiableBy(String workspaceKey) {
		Workspace workspace = getBy(workspaceKey);

		if (workspace.isArchived()) {
			throw new WorkspaceArchivedException(workspaceKey);
		}

		return workspace;
	}

	// TODO: add javadoc for the following information
	//  - its only for query API's
	public Workspace getBy(String workspaceKey) {
		return workspaceCommandRepository.findByKey(workspaceKey)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
	}
}
