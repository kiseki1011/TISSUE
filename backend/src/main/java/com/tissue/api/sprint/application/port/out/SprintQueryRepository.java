package com.tissue.api.sprint.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.enums.SprintStatus;

public interface SprintQueryRepository extends Repository<Sprint, Long> {

	Optional<Sprint> findById(Long id);

	Optional<Sprint> findByIdAndProject(Long id, Project project);

	Optional<Sprint> findByIdAndProjectKey(Long id, String projectKey);

	Optional<Sprint> findByProjectAndStatus(Project project, SprintStatus status);

	boolean existsByProjectAndStatus(Project project, SprintStatus status);
}
