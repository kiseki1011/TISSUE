package com.tissue.feature.sprint.adapter.web.request;

import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.GOAL_MAX_LENGTH;
import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.sprint.domain.policy.SprintConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.sprint.application.dto.request.CreateSprintCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSprintRequest(
        @NotBlank @Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH)
        String title,

        @Size(max = GOAL_MAX_LENGTH) String goal) {

    public CreateSprintCommand toCommand() {
        return new CreateSprintCommand(title, goal);
    }
}
