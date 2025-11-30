package com.tissue.api.sprint.application.service.finder;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.enums.SprintStatus;
import com.tissue.api.sprint.domain.exception.SprintNotFoundException;
import com.tissue.api.sprint.domain.port.out.SprintQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintFinder {

	private final SprintQueryRepository sprintQueryRepository;

	public Sprint findBy(Long sprintId, Project project) {
		return sprintQueryRepository.findByIdAndProject(sprintId, project)
			.orElseThrow(() -> new SprintNotFoundException(sprintId, project.getKey(), project.getWorkspaceKey()));
	}

	public Sprint findBy(Long sprintId, String projectKey, String workspaceKey) {
		return sprintQueryRepository.findByIdAndProjectKey(sprintId, projectKey)
			.orElseThrow(() -> new SprintNotFoundException(sprintId, projectKey, workspaceKey));
	}

	public Optional<Sprint> findActiveBy(Project project) {
		return sprintQueryRepository.findByProjectAndStatus(project, SprintStatus.ACTIVE);
	}

	public boolean existsActiveSprintByProject(Project project) {
		return sprintQueryRepository.existsByProjectAndStatus(project, SprintStatus.ACTIVE);
	}
}
