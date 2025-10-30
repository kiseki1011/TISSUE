package com.tissue.api.issue.application.port.out;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.IssueFieldValue;

public interface IssueFieldValueQueryRepository extends Repository<IssueFieldValue, Long> {

	@Query("""
		    SELECT fv
		    FROM IssueFieldValue fv
		    JOIN FETCH fv.field f
		    LEFT JOIN FETCH fv.enumOption eo
		    JOIN fv.issue i
		    JOIN i.workspace w
		    WHERE w.key = :workspaceKey
		      AND i.key = :issueKey
		    ORDER BY f.id
		""")
	List<IssueFieldValue> findByIssue(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);
}
