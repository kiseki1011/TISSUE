package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.application.dto.request.StatsWindowType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Cycle- and lead-time statistics over a window, computed from the project's issues that are currently in
 * a COMPLETED state and were resolved within the window. Cycle time measures start-of-work to resolution
 * (issues that never started are excluded); lead time measures creation to resolution.
 *
 * <p>Durations are calendar time: startedAt is the issue's first start (it is not reset on a reopen) and
 * resolvedAt is its latest resolution, so a reopened-and-re-resolved issue's cycle and lead times span the
 * reopen, including any dormant period it sat completed between resolutions. This is the same
 * first-start-to-final-resolution basis as the per-member average resolution time.
 */
public record ProjectCycleTimeStats(
        @Schema(description = "Inclusive start of the window")
        Instant from,

        @Schema(description = "Exclusive end of the window") Instant to,

        @Schema(description = "The preset that produced this window")
        StatsWindowType window,

        @Schema(description = "Start-of-work to resolution") DurationStats cycleTime,
        @Schema(description = "Creation to resolution") DurationStats leadTime) {

    public record DurationStats(
            @Schema(description = "Number of resolved issues the stat is computed over")
            long count,

            @Schema(description = "Mean duration in seconds (0 when count is 0)")
            long avgSeconds,

            @Schema(description = "Median (50th percentile) duration in seconds")
            long p50Seconds,

            @Schema(description = "90th percentile duration in seconds")
            long p90Seconds) {

        /** Builds the stat from a list of durations in seconds (empty yields all-zero). */
        public static DurationStats of(List<Long> seconds) {
            if (seconds.isEmpty()) {
                return new DurationStats(0, 0, 0, 0);
            }
            long[] sorted = seconds.stream().mapToLong(Long::longValue).sorted().toArray();
            long sum = 0;
            for (long s : sorted) {
                sum += s;
            }
            long avg = Math.round((double) sum / sorted.length);
            return new DurationStats(sorted.length, avg, percentile(sorted, 0.5), percentile(sorted, 0.9));
        }

        // Linear-interpolation percentile over an ascending-sorted array (matches the common "percentile"
        // definition; p in [0,1]).
        private static long percentile(long[] sortedAsc, double p) {
            double rank = p * (sortedAsc.length - 1);
            int lo = (int) Math.floor(rank);
            int hi = (int) Math.ceil(rank);
            if (lo == hi) {
                return sortedAsc[lo];
            }
            double value = sortedAsc[lo] + (rank - lo) * (double) (sortedAsc[hi] - sortedAsc[lo]);
            return Math.round(value);
        }
    }
}
