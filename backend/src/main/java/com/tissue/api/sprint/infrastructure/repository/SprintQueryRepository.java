package com.tissue.api.sprint.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.presentation.condition.SprintIssueSearchCondition;
import com.tissue.api.sprint.presentation.condition.SprintSearchCondition;

public interface SprintQueryRepository extends JpaRepository<Sprint, Long> {

	@Query("SELECT s FROM Sprint s "
		+ "LEFT JOIN FETCH s.sprintIssues si "
		+ "LEFT JOIN FETCH si.issue "
		+ "WHERE s.sprintKey = :sprintKey AND s.workspaceCode = :workspaceCode")
	Optional<Sprint> findBySprintKeyAndWorkspaceCodeWithIssues(
		@Param("sprintKey") String sprintKey,
		@Param("workspaceCode") String workspaceCode
	);

	@Query("SELECT s FROM Sprint s "
		+ "WHERE s.workspaceCode = :workspaceCode "
		+ "AND (:#{#condition.statuses == null || #condition.statuses.isEmpty()} = true "
		+ "     OR s.status IN :#{#condition.statuses}) "
		+ "AND (:#{#condition.keyword} IS NULL "
		+ "     OR LOWER(s.sprintKey) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')) "
		+ "     OR LOWER(s.title) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')) "
		+ "     OR LOWER(s.goal) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')))")
	Page<Sprint> findSprintPageByWorkspaceCode(
		@Param("workspaceCode") String workspaceCode,
		@Param("condition") SprintSearchCondition condition,
		Pageable pageable
	);

	@Query("SELECT si.issue FROM Sprint s "
		+ "JOIN s.sprintIssues si "
		+ "WHERE s.sprintKey = :sprintKey "
		+ "AND s.workspaceCode = :workspaceCode "
		+ "AND (:#{#condition.statuses == null || #condition.statuses.isEmpty()} = true"
		+ " OR si.issue.status IN :#{#condition.statuses}) "
		+ "AND (:#{#condition.types == null || #condition.types.isEmpty()} = true"
		+ " OR si.issue.type IN :#{#condition.types}) "
		+ "AND (:#{#condition.priorities == null || #condition.priorities.isEmpty()} = true"
		+ " OR si.issue.priority IN :#{#condition.priorities}) "
		+ "AND (:#{#condition.keyword} IS NULL "
		+ "    OR LOWER(si.issue.issueKey) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')) "
		+ "    OR LOWER(si.issue.title) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')) "
		+ "    OR LOWER(FUNCTION('TO_CHAR', si.issue.content)) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')))")
	Page<Issue> findIssuesInSprint(
		@Param("sprintKey") String sprintKey,
		@Param("workspaceCode") String workspaceCode,
		@Param("condition") SprintIssueSearchCondition condition,
		Pageable pageable
	);
}
