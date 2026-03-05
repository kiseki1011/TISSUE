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
                JOIN FETCH si.issueType sit
                JOIN FETCH si.currentState scs
                JOIN FETCH r.targetIssue ti
                JOIN FETCH ti.issueType tit
                JOIN FETCH ti.currentState tcs
                WHERE (si.workspaceKey = :workspaceKey AND si.key.value = :issueKey)
                   OR (ti.workspaceKey = :workspaceKey AND ti.key.value = :issueKey)
            """)
    List<IssueRelation> findAllRelations(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("""
                SELECT r
                FROM IssueRelation r
                JOIN FETCH r.sourceIssue si
                JOIN FETCH r.targetIssue ti
                WHERE si.workspaceKey = :workspaceKey
                  AND si.key.value = :issueKey
            """)
    List<IssueRelation> findBySourceIssue(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("""
                SELECT r
                FROM IssueRelation r
                JOIN FETCH r.sourceIssue si
                JOIN FETCH r.targetIssue ti
                WHERE ti.workspaceKey = :workspaceKey
                  AND ti.key.value = :issueKey
            """)
    List<IssueRelation> findByTargetIssue(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("""
                SELECT COUNT(r) > 0
                FROM IssueRelation r
                JOIN r.sourceIssue si
                JOIN r.targetIssue ti
                WHERE si.workspaceKey = :workspaceKey
                  AND si.key.value = :sourceIssueKey
                  AND ti.key.value = :targetIssueKey
            """)
    boolean existsRelation(
            @Param("workspaceKey") String workspaceKey,
            @Param("sourceIssueKey") String sourceIssueKey,
            @Param("targetIssueKey") String targetIssueKey);
}
