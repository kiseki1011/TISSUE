package com.tissue.api.issue.application.port.out;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.IssueRelation;

public interface IssueRelationQueryRepository extends Repository<IssueRelation, Long> {

	@Query("""
		    SELECT r
		    FROM IssueRelation r
		    JOIN FETCH r.sourceIssue si
		    JOIN FETCH si.issueType sit
		    JOIN FETCH si.currentState scs
		    JOIN FETCH r.targetIssue ti
		    JOIN FETCH ti.issueType tit
		    JOIN FETCH ti.currentState tcs
		    WHERE (si.workspaceKey = :workspaceKey AND si.key = :issueKey)
		       OR (ti.workspaceKey = :workspaceKey AND ti.key = :issueKey)
		""")
	List<IssueRelation> findAllRelations(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT r
		    FROM IssueRelation r
		    JOIN FETCH r.sourceIssue si
		    JOIN FETCH r.targetIssue ti
		    JOIN FETCH si.project p
		    JOIN FETCH p.workspace w
		    WHERE w.key = :workspaceKey
		      AND si.key = :issueKey
		""")
	List<IssueRelation> findBySourceIssue(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT r
		    FROM IssueRelation r
		    JOIN FETCH r.sourceIssue si
		    JOIN FETCH r.targetIssue ti
		    JOIN FETCH ti.project p
		    JOIN FETCH p.workspace w
		    WHERE w.key = :workspaceKey
		      AND ti.key = :issueKey
		""")
	List<IssueRelation> findByTargetIssue(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT COUNT(r) > 0
		    FROM IssueRelation r
		    JOIN r.sourceIssue si
		    JOIN r.targetIssue ti
		    JOIN si.project p
		    JOIN p.workspace w
		    WHERE w.key = :workspaceKey
		      AND si.key = :sourceIssueKey
		      AND ti.key = :targetIssueKey
		""")
	boolean existsRelation(
		@Param("workspaceKey") String workspaceKey,
		@Param("sourceIssueKey") String sourceIssueKey,
		@Param("targetIssueKey") String targetIssueKey
	);
}
