package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only aggregation queries backing the project statistics dashboard.
 *
 * <p>All queries are scoped by {@code project_id} and stats are count-based. The category, hierarchy
 * and priority breakdowns return only non-empty buckets. Soft-deleted issues are excluded.
 */
public interface IssueStatsQueryRepository extends Repository<Issue, Long> {

    @Query("""
            SELECT i.currentState.category AS category, COUNT(i) AS count
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
            GROUP BY i.currentState.category
        """)
    List<StateCategoryCountRow> countByStateCategory(@Param("projectId") Long projectId);

    @Query("""
            SELECT i.issueType.issueHierarchy AS hierarchy, COUNT(i) AS count
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
            GROUP BY i.issueType.issueHierarchy
        """)
    List<HierarchyCountRow> countByHierarchy(@Param("projectId") Long projectId);

    @Query("""
            SELECT i.priority AS priority, COUNT(i) AS count
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
            GROUP BY i.priority
        """)
    List<PriorityCountRow> countByPriority(@Param("projectId") Long projectId);

    @Query("""
            SELECT
                COALESCE(SUM(CASE WHEN i.participants.assignee IS NULL THEN 1 ELSE 0 END), 0) AS unassigned,
                COALESCE(SUM(CASE WHEN i.schedule.dueAt IS NOT NULL
                                  AND i.schedule.dueAt < :now
                                  AND i.currentState.category NOT IN :terminalCategories
                             THEN 1 ELSE 0 END), 0) AS overdue
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
        """)
    ProjectStatsKpiRow getKpis(
            @Param("projectId") Long projectId,
            @Param("now") Instant now,
            @Param("terminalCategories") Collection<StateCategory> terminalCategories);
}
