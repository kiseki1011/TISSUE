package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.IssueRelation;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueRelationQueryRepository extends Repository<IssueRelation, Long> {

    @Query("""
                SELECT r
                FROM IssueRelation r
                JOIN FETCH r.sourceIssue si
                JOIN FETCH r.targetIssue ti
                WHERE si.key.value = :issueKey
            """)
    List<IssueRelation> findBySourceIssueKey(@Param("issueKey") String issueKey);

    @Query("""
                SELECT r
                FROM IssueRelation r
                JOIN FETCH r.sourceIssue si
                JOIN FETCH r.targetIssue ti
                WHERE ti.key.value = :issueKey
            """)
    List<IssueRelation> findByTargetIssueKey(@Param("issueKey") String issueKey);
}
