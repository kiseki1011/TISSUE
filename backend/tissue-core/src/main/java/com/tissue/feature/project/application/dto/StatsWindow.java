package com.tissue.feature.project.application.dto;

import java.time.Duration;
import java.time.Instant;

/**
 * A resolved statistics time window, half-open {@code [from, to)}. Presets are resolved to concrete
 * instants by the service (WEEK/MONTH are rolling from now; SPRINT spans the sprint's active period).
 */
public record StatsWindow(Instant from, Instant to) {

    public static StatsWindow lastDays(int days, Instant now) {
        return new StatsWindow(now.minus(Duration.ofDays(days)), now);
    }

    public static StatsWindow between(Instant from, Instant to) {
        return new StatsWindow(from, to);
    }
}
