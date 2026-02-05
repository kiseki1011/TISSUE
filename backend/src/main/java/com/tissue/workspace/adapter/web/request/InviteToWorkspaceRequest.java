package com.tissue.workspace.adapter.web.request;

import com.tissue.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record InviteToWorkspaceRequest(
        @NotEmpty Set<@Email @NotBlank String> emails,
        @NotNull WorkspaceRole role,
        Set<String> targetProjectKeys) {

    public InviteToWorkspaceCommand toCommand() {
        return new InviteToWorkspaceCommand(emails, role, targetProjectKeys);
    }
}
