package com.tissue.sprint.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.time.Instant;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateSprintCommand(
        Long sprintId,
        JsonNullable<String> title,
        JsonNullable<String> goal,
        JsonNullable<Instant> startedAt,
        JsonNullable<Instant> dueAt,
        ProjectMemberContext actorContext) {}
