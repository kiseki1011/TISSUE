package com.tissue.api.issue.application.port.out;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueFieldValue;
import com.tissue.api.issuetype.domain.IssueField;

public interface IssueFieldValueQueryRepository extends Repository<IssueFieldValue, Long> {

	List<IssueFieldValue> findByIssue(Issue issue);

	boolean existsByField(IssueField field);

	@Query("""
		    SELECT fv
		    FROM IssueFieldValue fv
		    JOIN FETCH fv.field f
		    LEFT JOIN FETCH fv.enumOption eo
		    JOIN fv.issue i
		    JOIN i.project p
		    JOIN p.workspace w
		    WHERE w.key = :workspaceKey AND i.key = :issueKey
		    ORDER BY f.id
		""")
	List<IssueFieldValue> findByWorkspaceKeyAndIssueKey(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);
}
