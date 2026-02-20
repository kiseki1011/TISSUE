package com.tissue.feature.project.web.request;

import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.KEY_MAX_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.KEY_MIN_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.KEY_REGEX;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.TITLE_MAX_LENGTH;
import static com.tissue.feature.project.domain.policy.ProjectConstraintPolicy.TITLE_MIN_LENGTH;

import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(min = KEY_MIN_LENGTH, max = KEY_MAX_LENGTH) @Pattern(regexp = KEY_REGEX)
        String projectKey,

        @NotBlank @Size(min = TITLE_MIN_LENGTH, max = TITLE_MAX_LENGTH)
        String title,

        @Size(max = DESCRIPTION_MAX_LENGTH) String description) {

    public CreateProjectCommand toCommand() {
        return CreateProjectCommand.builder()
                .projectKey(projectKey)
                .title(title)
                .description(description)
                .build();
    }
}
