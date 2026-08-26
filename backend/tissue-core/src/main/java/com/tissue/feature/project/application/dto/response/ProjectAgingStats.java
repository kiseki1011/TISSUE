package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.issue.application.port.repository.ProjectAgingRow;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aging and flow-risk snapshot for a project's open issues: how long open work has been sitting (bucketed
 * by age) plus how many open issues are currently blocked. Always computed as of now; not windowed.
 */
public record ProjectAgingStats(
        @Schema(description = "Total open issues (INITIAL + ACTIVE); the sum of the age buckets")
        long openTotal,

        @Schema(description = "Open issues aged under 3 days (since work started, or since creation when not started)")
        long agingUnder3d,

        @Schema(description = "Open issues aged 3 to 7 days")
        long aging3to7d,

        @Schema(description = "Open issues aged 1 to 2 weeks (7 to 14 days)")
        long aging1to2w,

        @Schema(description = "Open issues aged over 2 weeks (14+ days)")
        long agingOver2w,

        @Schema(description = "Open issues currently blocked by a BLOCKS relation from another still-open issue")
        long blocked) {

    public static ProjectAgingStats of(ProjectAgingRow buckets, long blocked) {
        long openTotal = buckets.getUnder3d() + buckets.getDays3to7() + buckets.getWeeks1to2() + buckets.getOver2w();
        return new ProjectAgingStats(
                openTotal,
                buckets.getUnder3d(),
                buckets.getDays3to7(),
                buckets.getWeeks1to2(),
                buckets.getOver2w(),
                blocked);
    }
}
