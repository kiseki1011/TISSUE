package com.tissue.api.sprint.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.sprint.domain.model.Sprint;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

	Optional<Sprint> findBySprintKeyAndWorkspaceCode(String sprintKey, String workspaceCode);

	@Query("SELECT s FROM Sprint s "
		+ "LEFT JOIN FETCH s.sprintIssues si "
		+ "LEFT JOIN FETCH si.issue "
		+ "WHERE s.sprintKey = :sprintKey AND s.workspace.key = :workspaceKey")
	Optional<Sprint> findBySprintKeyAndWorkspaceKeyWithIssues(
		@Param("sprintKey") String sprintKey,
		@Param("workspaceKey") String workspaceKey
	);
}
