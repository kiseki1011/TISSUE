package com.tissue.api.issue.domain.port.out;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.IssueReviewer;

public interface IssueReviewerQueryRepository extends Repository<IssueReviewer, Long> {

	// TODO: 성능 최적화
	@Query("""
		    SELECT r
		    FROM IssueReviewer r
		    JOIN FETCH r.reviewer pm
		    JOIN FETCH pm.workspaceMember wm
		    JOIN FETCH wm.member m
		    JOIN r.issue i
		    JOIN i.project p
		    JOIN p.workspace w
		    WHERE w.key = :workspaceKey
		      AND i.key = :issueKey
		""")
	List<IssueReviewer> findByIssue(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);
}
