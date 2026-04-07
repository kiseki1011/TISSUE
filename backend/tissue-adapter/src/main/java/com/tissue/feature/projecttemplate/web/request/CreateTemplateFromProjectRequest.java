package com.tissue.feature.projecttemplate.web.request;

import com.tissue.feature.projecttemplate.application.dto.request.CreateTemplateFromProjectCommand;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record CreateTemplateFromProjectRequest(
        @NotBlank String projectKey,
        @NotBlank String name,
        @Nullable String description) {

    public CreateTemplateFromProjectCommand toCommand(String workspaceKey) {
        return CreateTemplateFromProjectCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .name(name)
                .description(description)
                .build();
    }
}
