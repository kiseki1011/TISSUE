package com.tissue.project.adapter.in.web.request;

import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @Size(min = 3, max = 12) @NotBlank String projectKey,
        @Size(min = 2, max = 100) @NotBlank String title,
        @Size(max = 255) String description) {

    public CreateProjectCommand toCommand(WorkspaceMemberContext actor) {
        return CreateProjectCommand.builder()
                .projectKey(projectKey)
                .title(title)
                .description(description)
                .actor(actor)
                .build();
    }
}
