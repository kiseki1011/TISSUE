package com.tissue.feature.project.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A per-member issue-resolution contribution heatmap ("잔디"): a dense, one-per-UTC-day series of how many
 * issues the member resolved in each day of the window, zero-filled so the client can render a continuous
 * calendar grid without reconstructing missing days.
 *
 * <p>"Resolved" is attributed by the issue's current assignee (matching the per-member stats), counting
 * issues currently in a COMPLETED state by their {@code resolvedAt} day - so a resolved-but-unassigned
 * issue is not counted and a reassignment after resolution follows the new assignee. {@code maxDaily} is
 * the busiest day's count, handed over so the client can scale the heatmap's shading.
 */
public record ProjectContributionStats(
        @Schema(description = "The member the heatmap is for")
        Long memberId,

        @Schema(description = "Window start (inclusive), UTC")
        Instant from,

        @Schema(description = "Window end (exclusive), UTC") Instant to,

        @Schema(description = "One entry per UTC day in the window, oldest first, zero-filled")
        List<ContributionDay> days,

        @Schema(description = "Total issues resolved across the window")
        long totalResolved,

        @Schema(description = "The busiest day's resolved count; 0 when the window is empty")
        long maxDaily) {

    public record ContributionDay(
            @Schema(description = "The calendar day, cut on the requested zone (UTC by default)")
            LocalDate date,

            @Schema(description = "Issues resolved on this day")
            long count) {}
}
