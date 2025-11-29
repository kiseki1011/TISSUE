package com.tissue.api.sprint.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;

// TODO: SprintQueryRepository로 변경, extends Repository 사용
// TODO: SprintCommandRepository 따로 만들기
public interface SprintRepository extends JpaRepository<Sprint, Long> {

	Optional<Sprint> findByIdAndProject(Long id, Project project);

	Optional<Sprint> findByIdAndProjectKey(Long id, String projectKey);

	Optional<Sprint> findByProjectAndStatus(Project project, SprintStatus status);

	boolean existsByProjectAndStatus(Project project, SprintStatus status);
}
