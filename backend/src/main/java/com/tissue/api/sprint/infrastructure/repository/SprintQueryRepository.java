package com.tissue.api.sprint.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;

public interface SprintQueryRepository extends Repository<Sprint, Long> {

	Optional<Sprint> findByIdAndProject(Long id, Project project);

	Optional<Sprint> findByIdAndProjectKey(Long id, String projectKey);

	Optional<Sprint> findByProjectAndStatus(Project project, SprintStatus status);

	boolean existsByProjectAndStatus(Project project, SprintStatus status);
}
