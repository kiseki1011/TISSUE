package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.issue.application.port.repository.ProjectMemberStatsRow;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

public record ProjectMemberStats(
        @Schema(description = "The member these stats are for")
        Long memberId,

        @Schema(description = "Issues assigned to the member that reached a `COMPLETED` state (excludes `ABORTED`)")
        long resolvedCount,

        @Schema(description = "Issues assigned to the member that are still open (`INITIAL` + `ACTIVE`)")
        long openAssignedCount,

        @Schema(description = "Sum of story points over the member's resolved issues (unset points count as 0)")
        long totalStoryPoints,

        @Schema(
                description =
                        "resolvedCount / (resolvedCount + openAssignedCount); 0 when the member has no assigned issues")
        double completionRate,

        @Schema(
                description = "Average seconds from creation to resolution over the member's resolved issues."
                        + " `null` when they have none")
        @Nullable
        Long avgResolveSeconds) {

    public static ProjectMemberStats from(ProjectMemberStatsRow row) {
        long resolved = row.getResolvedCount();
        long open = row.getOpenAssignedCount();
        long denominator = resolved + open;
        double rate = denominator == 0 ? 0.0 : (double) resolved / denominator;
        Double avgSeconds = row.getAvgResolveSeconds();
        Long avgResolveSeconds = avgSeconds == null ? null : Math.round(avgSeconds);
        return new ProjectMemberStats(
                row.getMemberId(), resolved, open, row.getTotalStoryPoints(), rate, avgResolveSeconds);
    }
}
