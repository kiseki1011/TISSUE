package com.tissue.sprint.application.dto.request;

import java.time.Instant;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateSprintCommand(
        JsonNullable<String> title,
        JsonNullable<String> goal,
        JsonNullable<Instant> startedAt,
        JsonNullable<Instant> dueAt) {}
