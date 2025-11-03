package com.tissue.api.issue.application.port.out;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.IssueReviewer;

public interface IssueReviewerQueryRepository extends Repository<IssueReviewer, Long> {

	@Query("""
		    SELECT r
		    FROM IssueReviewer r
		    JOIN FETCH r.reviewer wm
		    JOIN FETCH wm.member m
		    JOIN r.issue i
		    JOIN i.workspace w
		    WHERE w.key = :workspaceKey 
		      AND i.key = :issueKey
		""")
	List<IssueReviewer> findByIssue(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);
}
