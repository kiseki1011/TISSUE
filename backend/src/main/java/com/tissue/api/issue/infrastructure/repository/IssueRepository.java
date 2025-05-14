package com.tissue.api.issue.infrastructure.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.model.Issue;

public interface IssueRepository extends JpaRepository<Issue, Long> {

	Optional<Issue> findByIssueKeyAndWorkspaceCode(String issueKey, String workspaceCode);

	/**
	 * 워크스페이스 코드와 이슈 ID로 이슈와 그 하위 이슈들을 함께 조회합니다.
	 * 부모 이슈 설정 시 N+1 문제를 방지하기 위해 fetch join을 사용합니다.
	 */
	@Query("SELECT i FROM Issue i "
		+ "LEFT JOIN FETCH i.childIssues "
		+ "WHERE i.workspaceCode = :workspaceCode "
		+ "AND i.id = :issueId")
	Optional<Issue> findByWorkspaceCodeAndIdWithChildren(
		@Param("workspaceCode") String workspaceCode,
		@Param("issueId") Long issueId
	);

	@Query("SELECT i FROM Issue i "
		+ "JOIN FETCH i.sprintIssues si "
		+ "JOIN FETCH si.sprint s "
		+ "WHERE s.sprintKey = :sprintKey "
		+ "AND s.workspaceCode = :workspaceCode "
		+ "AND i.issueKey = :issueKey")
	Optional<Issue> findIssueInSprint(
		@Param("sprintKey") String sprintKey,
		@Param("issueKey") String issueKey,
		@Param("workspaceCode") String workspaceCode
	);

	List<Issue> findByIssueKeyInAndWorkspaceCode(Collection<String> issueKeys, String workspaceCode);

	/**
	 * 워크스페이스의 이슈들을 페이징하여 조회합니다.
	 * 추후 이슈 목록 조회 기능 구현 시 사용할 수 있습니다.
	 */
	Page<Issue> findByWorkspaceCode(String workspaceCode, Pageable pageable);
}
