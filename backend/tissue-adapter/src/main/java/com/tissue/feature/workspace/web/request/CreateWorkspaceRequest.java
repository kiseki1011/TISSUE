package com.tissue.feature.workspace.web.request;

import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.KEY_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.KEY_MIN_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.KEY_REGEX;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workspace.domain.policy.WorkspaceConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank @Size(min = KEY_MIN_LENGTH, max = KEY_MAX_LENGTH) @Pattern(regexp = KEY_REGEX)
        String workspaceKey,

        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Size(max = DESCRIPTION_MAX_LENGTH) String description) {

    public CreateWorkspaceCommand toCommand() {
        return new CreateWorkspaceCommand(workspaceKey, name, description);
    }
}
