package com.tissue.sprint.adapter.in.web.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.sprint.application.dto.request.CreateSprintCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSprintRequest(
        @Size(max = 50) @NotBlank String title,
        @Size(max = 255) String goal) {
    public CreateSprintCommand toCommand(ProjectMemberContext actorContext) {
        return new CreateSprintCommand(title, goal, actorContext);
    }
}
