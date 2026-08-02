package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Snapshot statistics for a project's current issues. Count-based, soft-deleted issues excluded.
 */
public record ProjectSimpleStats(
        @Schema(description = "Total non-deleted issues in the project")
        long total,

        @Schema(description = "Open issues (`INITIAL` + `ACTIVE`)")
        long open,

        @Schema(description = "Completed issues (`COMPLETED` only, excludes `ABORTED`)")
        long completed,

        @Schema(description = "Issues with no assignee") long unassigned,

        @Schema(description = "Open issues past their due date")
        long overdue,

        @Schema(description = "Issue count per state category, always 4 buckets in lifecycle order")
        List<CategoryCount> byStateCategory,

        @Schema(description = "Issue count per hierarchy, always 4 buckets from `EPIC` to `MICROTASK`")
        List<HierarchyCount> byHierarchy,

        @Schema(description = "Issue count per priority, always 5 buckets from `P0` to `P4`")
        List<PriorityCount> byPriority) {

    public record CategoryCount(StateCategory category, long count) {}

    public record HierarchyCount(IssueHierarchy hierarchy, long count) {}

    public record PriorityCount(IssuePriority priority, long count) {}

    public static ProjectSimpleStats of(
            Map<StateCategory, Long> categoryCounts,
            Map<IssueHierarchy, Long> hierarchyCounts,
            Map<IssuePriority, Long> priorityCounts,
            long unassigned,
            long overdue) {

        long total = categoryCounts.values().stream().mapToLong(Long::longValue).sum();
        long open = categoryCounts.getOrDefault(StateCategory.INITIAL, 0L)
                + categoryCounts.getOrDefault(StateCategory.ACTIVE, 0L);
        long completed = categoryCounts.getOrDefault(StateCategory.COMPLETED, 0L);

        List<CategoryCount> byStateCategory = Arrays.stream(StateCategory.values())
                .map(category -> new CategoryCount(category, categoryCounts.getOrDefault(category, 0L)))
                .toList();
        List<HierarchyCount> byHierarchy = Arrays.stream(IssueHierarchy.values())
                .map(hierarchy -> new HierarchyCount(hierarchy, hierarchyCounts.getOrDefault(hierarchy, 0L)))
                .toList();
        List<PriorityCount> byPriority = Arrays.stream(IssuePriority.values())
                .map(priority -> new PriorityCount(priority, priorityCounts.getOrDefault(priority, 0L)))
                .toList();

        return new ProjectSimpleStats(
                total, open, completed, unassigned, overdue, byStateCategory, byHierarchy, byPriority);
    }
}
