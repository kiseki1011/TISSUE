package com.tissue.sprint.adapter.in.web.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.sprint.application.dto.request.UpdateSprintCommand;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateSprintRequest(
        JsonNullable<@Size(max = 50) String> title,
        JsonNullable<@Size(max = 255) String> goal,
        JsonNullable<Instant> startedAt,
        JsonNullable<Instant> dueAt) {
    public UpdateSprintCommand toCommand(Long sprintId, ProjectMemberContext actorContext) {
        return UpdateSprintCommand.builder()
                .sprintId(sprintId)
                .title(title)
                .goal(goal)
                .startedAt(startedAt)
                .dueAt(dueAt)
                .actorContext(actorContext)
                .build();
    }
}
