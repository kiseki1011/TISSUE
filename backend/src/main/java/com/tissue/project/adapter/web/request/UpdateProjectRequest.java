package com.tissue.project.adapter.web.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateProjectRequest(
        JsonNullable<@Size(min = 2, max = 100) @NotBlank String> title,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<ProjectVisibility> projectVisibility,
        JsonNullable<ProjectRole> defaultJoinRole) {

    public UpdateProjectCommand toCommand(ProjectMemberContext actor) {
        return UpdateProjectCommand.builder()
                .title(title)
                .description(description)
                .projectVisibility(projectVisibility)
                .defaultJoinRole(defaultJoinRole)
                .actor(actor)
                .build();
    }
}
