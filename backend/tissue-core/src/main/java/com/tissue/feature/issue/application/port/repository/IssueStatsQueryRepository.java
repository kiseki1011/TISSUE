package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Read-only aggregation queries backing the project statistics dashboard.
 *
 * <p>A plain application port, deliberately decoupled from any persistence mechanism: statistics
 * queries change technology often (JPQL today, native SQL or a dedicated read model tomorrow), so
 * callers depend on this interface and never on Spring Data. The JPA-backed implementation lives in
 * {@code adapter/persistence} ({@code IssueStatsQueryAdapter}).
 *
 * <p>All queries are scoped by {@code project_id} and stats are count-based. The category, hierarchy
 * and priority breakdowns return only non-empty buckets. Soft-deleted issues are excluded.
 */
public interface IssueStatsQueryRepository {

    List<StateCategoryCountRow> countByStateCategory(Long projectId);

    List<HierarchyCountRow> countByHierarchy(Long projectId);

    List<PriorityCountRow> countByPriority(Long projectId);

    ProjectStatsKpiRow getKpis(Long projectId, Instant now, Collection<StateCategory> terminalCategories);

    List<ProjectMemberStatsRow> getMemberStats(
            Long projectId, StateCategory completed, Collection<StateCategory> openCategories);

    /**
     * Buckets the project's open issues by age. The three thresholds are the instants 3, 7 and 14 days
     * before now; an issue's age is measured from when it started (or its creation when not yet started).
     */
    ProjectAgingRow getAgingBuckets(
            Long projectId,
            Instant threshold3d,
            Instant threshold7d,
            Instant threshold14d,
            Collection<StateCategory> openCategories);

    /** Counts open issues that a BLOCKS relation from another still-open issue is currently blocking. */
    long countBlockedOpen(Long projectId, Collection<StateCategory> openCategories);

    /** Creation instants of the project's non-deleted issues created within {@code [from, to)}. */
    List<Instant> findCreatedAtBetween(Long projectId, Instant from, Instant to);

    /**
     * Resolution instants of the project's issues that are currently in the given (completed) category and
     * were resolved within {@code [from, to)}. Reopened issues (their resolvedAt cleared) are excluded.
     *
     * <p>resolvedAt records when an issue first entered a terminal state and is not refreshed on a later
     * re-completion, so an issue completed, then moved to an aborted state, then completed again keeps its
     * original timestamp - it may bucket on the earlier day or fall outside a recent window. This only
     * arises in workflows that permit terminal-to-terminal transitions.
     */
    List<Instant> findResolvedAtBetween(Long projectId, Instant from, Instant to, StateCategory completed);

    /**
     * Started/resolved instant pairs for cycle time (start of work to resolution), over issues currently in
     * the completed category and resolved within {@code [from, to)}. Issues that never started (no
     * startedAt) are excluded, since they have no cycle time.
     *
     * <p>startedAt is the first start (never reset) and resolvedAt the latest resolution, so a
     * reopened-and-re-resolved issue's pair spans the reopen (calendar time including the dormant period).
     */
    List<TimestampPairRow> findCycleTimePairs(Long projectId, Instant from, Instant to, StateCategory completed);

    /**
     * Created/resolved instant pairs for lead time (creation to resolution), over issues currently in the
     * completed category and resolved within {@code [from, to)}.
     */
    List<TimestampPairRow> findLeadTimePairs(Long projectId, Instant from, Instant to, StateCategory completed);

    /**
     * Per-state-category counts and story-point sums over the issues currently assigned to {@code sprintId}
     * (its live scope). Story points exclude EPIC issues, whose points are a rollup of their STANDARD
     * children and would double-count when both are in the sprint. Only non-empty categories are returned.
     */
    List<SprintStateAggregateRow> getSprintStateAggregates(Long sprintId);

    /**
     * Per-sprint velocity over a project's COMPLETED sprints: completed issue counts and completed story
     * points (excluding EPICs), ordered by sprint number. Reflects each issue's current sprint membership
     * and state, so a sprint whose scope changed after it closed reports its live tally, not a frozen
     * baseline. Completed sprints that hold no issue at all are omitted.
     */
    List<VelocitySprintRow> getVelocityBySprint(Long projectId, StateCategory completed);

    /**
     * Resolution instants of the project's issues currently in the given (completed) category, resolved
     * within {@code [from, to)} and assigned to {@code memberId}. Attribution is by the issue's current
     * assignee (matching the per-member stats), so a resolved-but-unassigned issue is not counted and a
     * reassignment after resolution follows the new assignee. Backs the per-member contribution heatmap.
     */
    List<Instant> findMemberResolvedAtBetween(
            Long projectId, Long memberId, Instant from, Instant to, StateCategory completed);
}
