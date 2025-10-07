package com.tissue.api.issue.base.infrastructure.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueType;

public interface IssueRepository extends JpaRepository<Issue, Long> {

	Optional<Issue> findByKeyAndWorkspace_Key(String issueKey, String workspaceKey);

	@Query("""
		select distinct i
		from Issue i
		join fetch i.sprintIssues si
		join fetch si.sprint s
		where s.key = :sprintKey
		  and s.workspace.key = :workspaceKey
		  and i.key = :issueKey
		""")
	Optional<Issue> findIssueInSprint(
		@Param("sprintKey") String sprintKey,
		@Param("issueKey") String issueKey,
		@Param("workspaceKey") String workspaceKey
	);

	List<Issue> findByKeyInAndWorkspace_Key(Collection<String> issueKeys, String workspaceKey);

	boolean existsByIssueType(IssueType issueType);
}
