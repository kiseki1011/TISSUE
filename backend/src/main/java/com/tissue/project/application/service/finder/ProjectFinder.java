package com.tissue.project.application.service.finder;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectExceptions;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectFinder {

	private final ProjectQueryRepository queryRepository;

	// TODO: add javadoc for the following information
	//  - its only for read-only API's
	public Project getBy(String projectKey, String workspaceKey) {
		// TODO: use JOIN FETCH with Workspace at findByKeyAndWorkspace_Key for optimization
		return queryRepository.findByKeyAndWorkspaceKey(projectKey, workspaceKey)
			.orElseThrow(() -> ProjectExceptions.notFound(workspaceKey, projectKey));
	}

	// TODO: add javadoc for the following information
	//  - its only for command API's
	//  - will throw an exception if workspace or project was archived
	public Project getModifiableBy(String projectKey, String workspaceKey) {
		Project project = getBy(projectKey, workspaceKey);

		if (project.getWorkspace().isArchived()) {
			throw WorkspaceExceptions.archived(project.getWorkspace());
		}
		if (project.isArchived()) {
			throw ProjectExceptions.isArchived(project);
		}

		return project;
	}

	public Optional<Project> findOptionalBy(Long projectId) {
		return queryRepository.findById(projectId);
	}
}
