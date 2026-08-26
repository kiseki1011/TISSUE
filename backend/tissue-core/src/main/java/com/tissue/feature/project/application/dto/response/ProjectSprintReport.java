package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.issue.application.port.repository.SprintStateAggregateRow;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A sprint report: a snapshot of a single sprint's current scope. The counts, story points and state
 * distribution reflect the issues assigned to the sprint <em>right now</em>, not the scope as it stood
 * when the sprint began - there is no scope history, so a "committed vs. delivered" burndown cannot be
 * reconstructed. For a COMPLETED sprint this snapshot is the final tally; for an ACTIVE sprint it is live
 * progress.
 *
 * <p>{@code completionRate} (completed issues over total) is the closest objective proxy for how much of
 * the sprint's work was delivered - the {@code goal} is free text with no structured target to measure
 * against. Story points exclude EPIC issues to avoid double-counting their rolled-up child points.
 */
public record ProjectSprintReport(
        @Schema(description = "The sprint's id") Long sprintId,

        @Schema(description = "Human-facing sprint key, e.g. S-1")
        String sprintKey,

        @Schema(description = "Sprint title") String title,

        @Schema(description = "Sprint goal (free text, may be empty)")
        String goal,

        @Schema(description = "Sprint lifecycle status") SprintStatus status,

        @Schema(description = "When the sprint started, if it has") @Nullable
        Instant startedAt,

        @Schema(description = "The sprint's planned due date, if set") @Nullable
        Instant dueAt,

        @Schema(description = "When the sprint was completed, if it has been") @Nullable
        Instant completedAt,

        @Schema(description = "Issues currently in the sprint's scope")
        long totalIssues,

        @Schema(description = "Issues in a COMPLETED state") long completedIssues,

        @Schema(description = "Still-open issues (INITIAL + ACTIVE) - the work that would carry over")
        long openIssues,

        @Schema(description = "Completed over total, in [0,1]; 0 when the sprint has no issues")
        double completionRate,

        @Schema(description = "Summed story points over the sprint's issues, excluding EPICs")
        long totalStoryPoints,

        @Schema(description = "Story points of the sprint's completed issues, excluding EPICs")
        long completedStoryPoints,

        @Schema(description = "Completed story points over total, in [0,1]; 0 when there are no points")
        double pointsCompletionRate,

        @Schema(description = "Issue counts per state category, for categories that have at least one issue")
        List<SprintStateCount> stateDistribution) {

    public record SprintStateCount(
            @Schema(description = "The state category") StateCategory category,

            @Schema(description = "Issues in this category within the sprint")
            long count) {}

    public static ProjectSprintReport of(Sprint sprint, List<SprintStateAggregateRow> rows) {
        long total = 0;
        long completed = 0;
        long open = 0;
        long totalPoints = 0;
        long completedPoints = 0;
        List<SprintStateCount> distribution = new ArrayList<>();
        for (SprintStateAggregateRow row : rows) {
            long count = row.getCount();
            long points = row.getStoryPoints();
            StateCategory category = row.getCategory();
            total += count;
            totalPoints += points;
            if (category == StateCategory.COMPLETED) {
                completed += count;
                completedPoints += points;
            }
            if (category == StateCategory.INITIAL || category == StateCategory.ACTIVE) {
                open += count;
            }
            distribution.add(new SprintStateCount(category, count));
        }
        double completionRate = total == 0 ? 0.0 : (double) completed / total;
        double pointsCompletionRate = totalPoints == 0 ? 0.0 : (double) completedPoints / totalPoints;
        return new ProjectSprintReport(
                sprint.getId(),
                sprint.getSprintKey(),
                sprint.getTitle(),
                sprint.getGoal(),
                sprint.getStatus(),
                sprint.getStartedAt(),
                sprint.getDueAt(),
                sprint.getCompletedAt(),
                total,
                completed,
                open,
                completionRate,
                totalPoints,
                completedPoints,
                pointsCompletionRate,
                distribution);
    }
}
