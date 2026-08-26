package com.tissue.feature.project.application.dto.request;

import java.util.Locale;

/** The preset time window for the project flow statistics. */
public enum StatsWindowType {
    WEEK,
    MONTH,
    SPRINT;

    /**
     * Parses a request value leniently. An unknown or blank value falls back to {@link #MONTH} so a
     * read-only stats endpoint never rejects on a typo.
     */
    public static StatsWindowType fromOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return MONTH;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MONTH;
        }
    }
}
