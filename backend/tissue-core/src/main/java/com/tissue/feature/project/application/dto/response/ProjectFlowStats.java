package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.application.dto.request.StatsWindowType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Flow statistics over a time window: a dense, one-per-day series of how many issues were created and how
 * many were resolved. "Resolved" counts issues currently in a COMPLETED state by their resolution date, so
 * a reopened issue drops out until it is resolved again. Days with no activity are present with zero counts.
 */
public record ProjectFlowStats(
        @Schema(description = "Inclusive start of the window")
        Instant from,

        @Schema(description = "Exclusive end of the window") Instant to,

        @Schema(description = "The preset that produced this window")
        StatsWindowType window,

        @Schema(description = "One point per UTC day in the window, in ascending date order")
        List<FlowPoint> points) {

    public record FlowPoint(
            @Schema(description = "The calendar day, cut on the requested zone (UTC by default)")
            LocalDate date,

            @Schema(description = "Issues created on this day")
            long created,

            @Schema(description = "Issues resolved (entered a COMPLETED state) on this day")
            long resolved) {}
}
