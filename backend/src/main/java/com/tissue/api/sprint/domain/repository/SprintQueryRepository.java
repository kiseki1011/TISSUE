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

public interface SprintQueryRepository extends JpaRepository<Sprint, Long> {

	@Query("SELECT s FROM Sprint s "
		+ "LEFT JOIN FETCH s.sprintIssues si "
		+ "LEFT JOIN FETCH si.issue "
		+ "WHERE s.sprintKey = :sprintKey AND s.workspaceCode = :workspaceCode")
	Optional<Sprint> findBySprintKeyAndWorkspaceCodeWithIssues(
		@Param("sprintKey") String sprintKey,
		@Param("workspaceCode") String workspaceCode
	);

	// @Query("SELECT si.issue FROM Sprint s "
	// 	+ "JOIN s.sprintIssues si " // 조인을 통해 Sprint에 속함 Issue들을 가져옴
	// 	+ "WHERE s.sprintKey = :sprintKey "
	// 	+ "AND s.workspaceCode = :workspaceCode " // 특정 sprintKey와 workspaceCode를 가진 Sprint만 조회
	// 	// statuses, types, priorities에 대한 필터링
	// 	+ "AND (COALESCE(:#{#condition.statuses}, NULL) IS NULL OR si.issue.status IN :#{#condition.statuses}) "
	// 	+ "AND (COALESCE(:#{#condition.types}, NULL) IS NULL OR si.issue.type IN :#{#condition.types}) "
	// 	+ "AND (COALESCE(:#{#condition.priorities}, NULL) IS NULL OR si.issue.priority IN :#{#condition.priorities}) "
	// 	// keyword를 통한 검색
	// 	+ "AND (:#{#condition.keyword} IS NULL "
	// 	+ "    OR LOWER(si.issue.issueKey) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')) "
	// 	+ "    OR LOWER(si.issue.title) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')) "
	// 	+ "    OR LOWER(FUNCTION('TO_CHAR', si.issue.content)) LIKE LOWER(CONCAT('%', :#{#condition.keyword}, '%')))")
	// Page<Issue> findIssuesInSprint(
	// 	@Param("sprintKey") String sprintKey,
	// 	@Param("workspaceCode") String workspaceCode,
	// 	@Param("condition") SprintIssueSearchCondition condition,
	// 	Pageable pageable
	// );

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
