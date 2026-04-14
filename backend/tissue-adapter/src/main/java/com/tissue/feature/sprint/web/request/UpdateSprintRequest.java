package com.tissue.feature.sprint.web.request;

import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.GOAL_MAX_LENGTH;
import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.sprint.application.dto.request.UpdateSprintCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateSprintRequest(
        @Schema(minLength = TITLE_MIN_LENGTH, maxLength = TITLE_MAX_LENGTH)
        JsonNullable<@Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH) String> title,

        @Schema(maxLength = GOAL_MAX_LENGTH) JsonNullable<@Size(max = GOAL_MAX_LENGTH) String> goal,
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
