package com.tissue.sprint.application.service.finder;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.project.domain.Project;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.enums.SprintStatus;
import com.tissue.sprint.domain.exception.SprintExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintFinder {

	private final SprintQueryRepository sprintQueryRepository;

	public Sprint findBy(Long sprintId, Project project) {
		return sprintQueryRepository.findByIdAndProject(sprintId, project)
			.orElseThrow(() -> SprintExceptions.notFound(sprintId, project));
	}

	public Optional<Sprint> findOptBy(Long sprintId, Project project) {
		return sprintQueryRepository.findByIdAndProject(sprintId, project);
	}

	public Sprint findBy(Long sprintId, String projectKey) {
		return sprintQueryRepository.findByIdAndProject_Key(sprintId, projectKey)
			.orElseThrow(() -> SprintExceptions.notFound(sprintId, projectKey));
	}

	public Optional<Sprint> findActiveBy(Project project) {
		return sprintQueryRepository.findByProjectAndStatus(project, SprintStatus.ACTIVE);
	}

	public boolean existsActiveSprintByProject(Project project) {
		return sprintQueryRepository.existsByProjectAndStatus(project, SprintStatus.ACTIVE);
	}
}
