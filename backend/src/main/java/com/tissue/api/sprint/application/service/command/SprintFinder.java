package com.tissue.api.sprint.application.service.command;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.sprint.infrastructure.repository.SprintQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintFinder {

	private final SprintQueryRepository sprintQueryRepository;

	public Sprint findBy(Long sprintId, Project project) {
		return sprintQueryRepository.findByIdAndProjectKey(sprintId, project.getKey())
			// TODO: SprintNotFoundException
			.orElseThrow(() -> new RuntimeException(
				"Sprint not found with sprint id '%d', project key '%s', workspace key '%s'."
					.formatted(sprintId, project.getKey(), project.getWorkspaceKey()))
			);
	}

	public Optional<Sprint> findActiveBy(Project project) {
		return sprintQueryRepository.findByProjectAndStatus(project, SprintStatus.ACTIVE);
	}

	public boolean existsActiveSprintByProject(Project project) {
		return sprintQueryRepository.existsByProjectAndStatus(project, SprintStatus.ACTIVE);
	}
}
