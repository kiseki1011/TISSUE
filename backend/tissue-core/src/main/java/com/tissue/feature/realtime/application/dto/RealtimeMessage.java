package com.tissue.feature.realtime.application.dto;

import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The JSON body of a realtime SSE frame.
 */
public record RealtimeMessage(
        String type,
        String projectKey,
        @Nullable String issueKey,
        @Nullable Long actorMemberId,
        Instant occurredAt,
        Map<String, Object> data) {}
