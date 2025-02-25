package com.tissue.api.sprint.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.presentation.condition.SprintIssueSearchCondition;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

	Optional<Sprint> findBySprintKeyAndWorkspaceCode(String sprintKey, String workspaceCode);

	// @Query("SELECT s FROM Sprint s "
	// 	+ "LEFT JOIN FETCH s.sprintIssues "
	// 	+ "WHERE s.sprintKey = :sprintKey AND s.workspaceCode = :workspaceCode")
	// Optional<Sprint> findBySprintKeyAndWorkspaceCodeWithIssues(
	// 	@Param("sprintKey") String sprintKey,
	// 	@Param("workspaceCode") String workspaceCode
	// );

	@Query("SELECT s FROM Sprint s "
		+ "LEFT JOIN FETCH s.sprintIssues si "
		+ "LEFT JOIN FETCH si.issue "
		+ "WHERE s.sprintKey = :sprintKey AND s.workspaceCode = :workspaceCode")
	Optional<Sprint> findBySprintKeyAndWorkspaceCodeWithIssues(
		@Param("sprintKey") String sprintKey,
		@Param("workspaceCode") String workspaceCode
	);

	@Query("SELECT si.issue FROM Sprint s "
		+ "JOIN s.sprintIssues si "
		+ "WHERE s.sprintKey = :sprintKey "
		+ "AND s.workspaceCode = :workspaceCode "
		+ "AND (:#{#condition.statuses.isEmpty()} = true OR si.issue.status IN :#{#condition.statuses}) "
		+ "AND (:#{#condition.types.isEmpty()} = true OR si.issue.type IN :#{#condition.types}) "
		+ "AND (:#{#condition.priorities.isEmpty()} = true OR si.issue.priority IN :#{#condition.priorities})")
	Page<Issue> findIssuesInSprint(
		@Param("sprintKey") String sprintKey,
		@Param("workspaceCode") String workspaceCode,
		@Param("condition") SprintIssueSearchCondition condition,
		Pageable pageable
	);
}
