package com.tissue.feature.workspace.web.request;

import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record InviteToWorkspaceRequest(
        @NotEmpty @Size(max = 50) Set<@Email @NotBlank String> emails,
        @NotNull WorkspaceRole role,
        Set<String> targetProjectKeys) {

    public InviteToWorkspaceCommand toCommand() {
        return new InviteToWorkspaceCommand(emails, role, targetProjectKeys);
    }
}
