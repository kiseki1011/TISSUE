package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.application.port.repository.HierarchyCountRow;
import com.tissue.feature.issue.application.port.repository.PriorityCountRow;
import com.tissue.feature.issue.application.port.repository.ProjectAgingRow;
import com.tissue.feature.issue.application.port.repository.ProjectMemberStatsRow;
import com.tissue.feature.issue.application.port.repository.ProjectStatsKpiRow;
import com.tissue.feature.issue.application.port.repository.SprintStateAggregateRow;
import com.tissue.feature.issue.application.port.repository.StateCategoryCountRow;
import com.tissue.feature.issue.application.port.repository.TimestampPairRow;
import com.tissue.feature.issue.application.port.repository.VelocitySprintRow;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data fragment holding the project-statistics JPQL. An internal detail of
 * {@link IssueStatsQueryAdapter}; application code depends on the
 * {@link com.tissue.feature.issue.application.port.repository.IssueStatsQueryRepository} port, not on
 * this interface, so the query mechanism can be swapped without touching callers.
 */
interface IssueStatsJpaRepository extends Repository<Issue, Long> {

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

    @Query("""
            SELECT
                a.member.id AS memberId,
                COALESCE(SUM(CASE WHEN i.currentState.category = :completed THEN 1 ELSE 0 END), 0) AS resolvedCount,
                COALESCE(SUM(CASE WHEN i.currentState.category IN :openCategories THEN 1 ELSE 0 END), 0)
                    AS openAssignedCount,
                COALESCE(SUM(CASE WHEN i.currentState.category = :completed
                                  THEN COALESCE(i.storyPoint, 0) ELSE 0 END), 0) AS totalStoryPoints,
                AVG(CASE WHEN i.currentState.category = :completed
                         THEN timestampdiff(SECOND, i.createdAt, i.schedule.resolvedAt) END) AS avgResolveSeconds
            FROM Issue i
            JOIN i.participants.assignee a
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
            GROUP BY a.member.id
        """)
    List<ProjectMemberStatsRow> getMemberStats(
            @Param("projectId") Long projectId,
            @Param("completed") StateCategory completed,
            @Param("openCategories") Collection<StateCategory> openCategories);

    @Query("""
            SELECT
                COALESCE(SUM(CASE WHEN COALESCE(i.schedule.startedAt, i.createdAt) > :threshold3d
                             THEN 1 ELSE 0 END), 0) AS under3d,
                COALESCE(SUM(CASE WHEN COALESCE(i.schedule.startedAt, i.createdAt) <= :threshold3d
                                  AND COALESCE(i.schedule.startedAt, i.createdAt) > :threshold7d
                             THEN 1 ELSE 0 END), 0) AS days3to7,
                COALESCE(SUM(CASE WHEN COALESCE(i.schedule.startedAt, i.createdAt) <= :threshold7d
                                  AND COALESCE(i.schedule.startedAt, i.createdAt) > :threshold14d
                             THEN 1 ELSE 0 END), 0) AS weeks1to2,
                COALESCE(SUM(CASE WHEN COALESCE(i.schedule.startedAt, i.createdAt) <= :threshold14d
                             THEN 1 ELSE 0 END), 0) AS over2w
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
              AND i.currentState.category IN :openCategories
        """)
    ProjectAgingRow getAgingBuckets(
            @Param("projectId") Long projectId,
            @Param("threshold3d") Instant threshold3d,
            @Param("threshold7d") Instant threshold7d,
            @Param("threshold14d") Instant threshold14d,
            @Param("openCategories") Collection<StateCategory> openCategories);

    @Query("""
            SELECT COUNT(DISTINCT r.targetIssue.id)
            FROM IssueRelation r
            WHERE r.relationType = com.tissue.feature.issue.domain.enums.IssueRelationType.BLOCKS
              AND r.targetIssue.project.id = :projectId
              AND r.targetIssue.softDeleted = false
              AND r.targetIssue.currentState.category IN :openCategories
              AND r.sourceIssue.softDeleted = false
              AND r.sourceIssue.currentState.category IN :openCategories
        """)
    long countBlockedOpen(
            @Param("projectId") Long projectId, @Param("openCategories") Collection<StateCategory> openCategories);

    @Query("""
            SELECT i.createdAt
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
              AND i.createdAt >= :from
              AND i.createdAt < :to
        """)
    List<Instant> findCreatedAtBetween(
            @Param("projectId") Long projectId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT i.schedule.resolvedAt
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
              AND i.currentState.category = :completed
              AND i.schedule.resolvedAt >= :from
              AND i.schedule.resolvedAt < :to
        """)
    List<Instant> findResolvedAtBetween(
            @Param("projectId") Long projectId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("completed") StateCategory completed);

    @Query("""
            SELECT i.schedule.startedAt AS startAt, i.schedule.resolvedAt AS endAt
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
              AND i.currentState.category = :completed
              AND i.schedule.startedAt IS NOT NULL
              AND i.schedule.resolvedAt >= :from
              AND i.schedule.resolvedAt < :to
        """)
    List<TimestampPairRow> findCycleTimePairs(
            @Param("projectId") Long projectId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("completed") StateCategory completed);

    @Query("""
            SELECT i.createdAt AS startAt, i.schedule.resolvedAt AS endAt
            FROM Issue i
            WHERE i.project.id = :projectId
              AND i.softDeleted = false
              AND i.currentState.category = :completed
              AND i.schedule.resolvedAt >= :from
              AND i.schedule.resolvedAt < :to
        """)
    List<TimestampPairRow> findLeadTimePairs(
            @Param("projectId") Long projectId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("completed") StateCategory completed);

    @Query("""
            SELECT
                i.currentState.category AS category,
                COUNT(i) AS count,
                COALESCE(SUM(CASE WHEN i.issueType.issueHierarchy
                                       <> com.tissue.feature.issue.domain.enums.IssueHierarchy.EPIC
                                  THEN COALESCE(i.storyPoint, 0) ELSE 0 END), 0) AS storyPoints
            FROM Issue i
            WHERE i.sprint.id = :sprintId
              AND i.softDeleted = false
            GROUP BY i.currentState.category
        """)
    List<SprintStateAggregateRow> getSprintStateAggregates(@Param("sprintId") Long sprintId);

    @Query("""
            SELECT
                s.id AS sprintId,
                s.sprintNumber AS sprintNumber,
                s.title AS title,
                s.completedAt AS completedAt,
                COALESCE(SUM(CASE WHEN i.currentState.category = :completed THEN 1 ELSE 0 END), 0) AS completedIssues,
                COALESCE(SUM(CASE WHEN i.currentState.category = :completed
                                  AND i.issueType.issueHierarchy
                                       <> com.tissue.feature.issue.domain.enums.IssueHierarchy.EPIC
                                  THEN COALESCE(i.storyPoint, 0) ELSE 0 END), 0) AS completedStoryPoints
            FROM Issue i
              JOIN i.sprint s
            WHERE s.project.id = :projectId
              AND s.status = com.tissue.feature.sprint.domain.SprintStatus.COMPLETED
              AND i.softDeleted = false
            GROUP BY s.id, s.sprintNumber, s.title, s.completedAt
            ORDER BY s.sprintNumber ASC
        """)
    List<VelocitySprintRow> getVelocityBySprint(
            @Param("projectId") Long projectId, @Param("completed") StateCategory completed);

    @Query("""
            SELECT i.schedule.resolvedAt
            FROM Issue i
              JOIN i.participants.assignee a
            WHERE i.project.id = :projectId
              AND a.member.id = :memberId
              AND i.softDeleted = false
              AND i.currentState.category = :completed
              AND i.schedule.resolvedAt >= :from
              AND i.schedule.resolvedAt < :to
        """)
    List<Instant> findMemberResolvedAtBetween(
            @Param("projectId") Long projectId,
            @Param("memberId") Long memberId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("completed") StateCategory completed);
}
