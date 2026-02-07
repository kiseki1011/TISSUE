package com.tissue.project.adapter.web.request;

import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.domain.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateProjectRequest(
        JsonNullable<@Size(min = 2, max = 100) @NotBlank String> title,
        JsonNullable<@Size(max = 255) String> description,
        JsonNullable<ProjectVisibility> projectVisibility) {

    public UpdateProjectCommand toCommand() {
        return UpdateProjectCommand.builder()
                .title(title)
                .description(description)
                .projectVisibility(projectVisibility)
                .build();
    }
}
