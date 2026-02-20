package com.tissue.feature.sprint.web.request;

import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.GOAL_MAX_LENGTH;
import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.sprint.application.dto.request.UpdateSprintCommand;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateSprintRequest(
        JsonNullable<@Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH) String> title,
        JsonNullable<@Size(max = GOAL_MAX_LENGTH) String> goal,
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
