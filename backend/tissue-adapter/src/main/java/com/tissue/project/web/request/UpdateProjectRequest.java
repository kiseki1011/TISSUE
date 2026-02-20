package com.tissue.project.web.request;

import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.project.application.dto.request.UpdateProjectCommand;
import com.tissue.feature.project.domain.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateProjectRequest(
        JsonNullable<@Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH) @NotBlank String> title,
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description,
        JsonNullable<ProjectVisibility> projectVisibility) {

    public UpdateProjectCommand toCommand() {
        return UpdateProjectCommand.builder()
                .title(title)
                .description(description)
                .projectVisibility(projectVisibility)
                .build();
    }
}
