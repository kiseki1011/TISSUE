package com.tissue.workspace.adapter.web.request;

import com.tissue.workspace.application.dto.request.CreateWorkspaceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @Size(min = 3, max = 21)
        @NotBlank
        @Pattern(
                regexp = "^(?!.*--)[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9]$",
                message = "Workspace key must start with a letter, end with a letter or number, "
                        + "and cannot contain consecutive hyphens.")
        String workspaceKey,

        @Size(max = 100) @NotBlank String name,
        @Size(max = 255) String description) {

    public CreateWorkspaceCommand toCommand() {
        return new CreateWorkspaceCommand(workspaceKey, name, description);
    }
}
