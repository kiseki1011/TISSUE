package com.tissue.feature.project.adapter.web.request;

import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.project.application.dto.request.UpdateProjectCommand;
import com.tissue.feature.project.domain.ProjectVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateProjectRequest(
        @Schema(minLength = TITLE_MIN_LENGTH, maxLength = TITLE_MAX_LENGTH)
        JsonNullable<@Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH) @NotBlank String> title,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
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
