package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.issue.application.port.repository.VelocitySprintRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A project's sprint velocity: the delivered work per COMPLETED sprint, oldest first. Each point is how
 * much of that sprint's scope ended up completed - story points (excluding EPICs, whose points roll up
 * their children) and issue count. {@code averageStoryPoints} is the mean over the returned sprints, the
 * usual basis for planning the next sprint's commitment.
 *
 * <p>This reflects each issue's <em>current</em> sprint membership and state, not the scope frozen at the
 * sprint's close (there is no scope history), so it mirrors the sprint report's live-snapshot limitation.
 */
public record ProjectVelocity(
        @Schema(description = "Completed sprints oldest first, one point each")
        List<VelocityPoint> sprints,

        @Schema(description = "Mean completed story points over the returned sprints; 0 when there are none")
        double averageStoryPoints,

        @Schema(description = "Mean completed issue count over the returned sprints; 0 when there are none")
        double averageCompletedIssues) {

    public record VelocityPoint(
            @Schema(description = "The sprint's id") Long sprintId,

            @Schema(description = "Human-facing sprint key, e.g. S-1")
            String sprintKey,

            @Schema(description = "Sprint title") String title,

            @Schema(description = "When the sprint was completed, if recorded") @Nullable
            Instant completedAt,

            @Schema(description = "Issues of this sprint now in a COMPLETED state")
            long completedIssues,

            @Schema(description = "Completed story points, excluding EPICs")
            long completedStoryPoints) {}

    public static ProjectVelocity of(List<VelocitySprintRow> rows) {
        List<VelocityPoint> points = rows.stream()
                .map(r -> new VelocityPoint(
                        r.getSprintId(),
                        "S-" + r.getSprintNumber(),
                        r.getTitle(),
                        r.getCompletedAt(),
                        r.getCompletedIssues(),
                        r.getCompletedStoryPoints()))
                .toList();
        double avgPoints = points.isEmpty()
                ? 0.0
                : points.stream().mapToLong(VelocityPoint::completedStoryPoints).sum() / (double) points.size();
        double avgIssues = points.isEmpty()
                ? 0.0
                : points.stream().mapToLong(VelocityPoint::completedIssues).sum() / (double) points.size();
        return new ProjectVelocity(points, avgPoints, avgIssues);
    }
}
