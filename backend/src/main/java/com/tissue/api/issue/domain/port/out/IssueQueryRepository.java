package com.tissue.api.issue.domain.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issuetype.domain.IssueType;

public interface IssueQueryRepository extends Repository<Issue, Long> {

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

	Optional<Issue> findByKeyAndWorkspaceKey(
		String issueKey,
		String workspaceKey
	);

	List<Issue> findByKeyInAndWorkspaceKey(
		Collection<String> issueKeys,
		String workspaceKey
	);

	@Query("""
		    SELECT i
		    FROM Issue i
		    JOIN FETCH i.project p
		    JOIN FETCH i.issueType it
		    JOIN FETCH it.workflow
		    JOIN FETCH i.currentState
		    WHERE p.workspaceKey = :workspaceKey AND i.key = :issueKey
		""")
	Optional<Issue> findWithBasicInfo(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT i
		    FROM Issue i
		    JOIN FETCH i.project p
		    JOIN FETCH i.issueType it
		    JOIN FETCH i.currentState cs
		    LEFT JOIN FETCH i.participants.assignee a
		    LEFT JOIN FETCH a.member am
		    JOIN FETCH i.participants.reporter r
		    JOIN FETCH r.member rm
		    WHERE p.workspaceKey = :workspaceKey AND i.key = :issueKey
		""")
	Optional<Issue> findWithDetail(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT i
		    FROM Issue i
		    JOIN FETCH i.project p
		    LEFT JOIN FETCH i.parentIssue pi
		    LEFT JOIN FETCH pi.issueType pit
		    WHERE p.workspaceKey = :workspaceKey AND i.key = :issueKey
		""")
	Optional<Issue> findWithParent(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT child
		    FROM Issue child
		    JOIN FETCH child.issueType it
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		    ORDER BY child.createdAt ASC
		""")
	List<Issue> findChildren(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT COUNT(child) > 0
		    FROM Issue child
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		""")
	boolean hasChildren(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	boolean existsByIssueType(IssueType issueType);

	@Query("""
		    SELECT COUNT(child)
		    FROM Issue child
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		""")
	int countChildren(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT COUNT(child)
		    FROM Issue child
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    JOIN child.currentState cs
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		      AND cs.category = 'DONE'
		""")
	int countCompletedChildren(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT COALESCE(SUM(child.storyPoint), 0)
		    FROM Issue child
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		      AND child.storyPoint IS NOT NULL
		""")
	int sumChildrenStoryPoints(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT COALESCE(SUM(child.storyPoint), 0)
		    FROM Issue child
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    JOIN child.currentState cs
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		      AND cs.category = 'DONE'
		      AND child.storyPoint IS NOT NULL
		""")
	int sumCompletedChildrenStoryPoints(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);
}
