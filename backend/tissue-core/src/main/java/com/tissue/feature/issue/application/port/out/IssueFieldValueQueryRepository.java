package com.tissue.feature.issue.application.port.out;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issuetype.domain.IssueField;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueFieldValueQueryRepository extends Repository<IssueFieldValue, Long> {

    List<IssueFieldValue> findByIssue(Issue issue);

    @Query("SELECT COUNT(fv) > 0 FROM IssueFieldValue fv WHERE fv.field = :field")
    boolean existsByField(@Param("field") IssueField field);

    @Query("""
                SELECT fv
                FROM IssueFieldValue fv
                JOIN FETCH fv.field f
                LEFT JOIN FETCH fv.enumOption eo
                JOIN fv.issue i
                JOIN i.project p
                JOIN p.workspace w
                WHERE w.key = :workspaceKey AND i.key.value = :issueKey
                ORDER BY f.id
            """)
    List<IssueFieldValue> findByWorkspaceKeyAndIssueKey(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);
}
