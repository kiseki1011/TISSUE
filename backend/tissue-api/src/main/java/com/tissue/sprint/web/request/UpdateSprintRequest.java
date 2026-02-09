package com.tissue.sprint.web.request;

import com.tissue.sprint.application.dto.request.UpdateSprintCommand;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateSprintRequest(
        JsonNullable<@Size(max = 50) String> title,
        JsonNullable<@Size(max = 255) String> goal,
        JsonNullable<Instant> startedAt,
        JsonNullable<Instant> dueAt) {

    public UpdateSprintCommand toCommand() {
        return UpdateSprintCommand.builder()
                .title(title)
                .goal(goal)
                .startedAt(startedAt)
                .dueAt(dueAt)
                .build();
    }
}
