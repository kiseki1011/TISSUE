package com.tissue.api.project.application.service.finder;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.project.application.port.out.ProjectQueryRepository;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.exception.ProjectArchivedException;
import com.tissue.api.project.domain.exception.ProjectNotFoundException;
import com.tissue.api.workspace.domain.exception.WorkspaceArchivedException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectFinder {

	private final ProjectQueryRepository queryRepository;

	// TODO: 읽기 전용 이라는 주석 추가
	public Project findBy(String projectKey, String workspaceKey) {
		// TODO: findByKeyAndWorkspace_Key가 Workspace 까지 JOIN FETCH로 가져오는 것을 고려(성능 최적화)
		return queryRepository.findByKeyAndWorkspace_Key(projectKey, workspaceKey)
			.orElseThrow(() -> new ProjectNotFoundException(projectKey, workspaceKey));
	}

	// TODO: 커맨드 전용 이라는 주석 추가
	public Project findForCommand(String projectKey, String workspaceKey) {
		Project project = findBy(projectKey, workspaceKey);

		if (project.isArchived()) {
			throw new ProjectArchivedException(project.getKey(), project.getWorkspaceKey());
		}
		if (project.getWorkspace().isArchived()) {
			throw new WorkspaceArchivedException(project.getWorkspaceKey());
		}

		return project;
	}

	public Optional<Project> findOptionalBy(Long projectId) {
		return queryRepository.findById(projectId);
	}
}
