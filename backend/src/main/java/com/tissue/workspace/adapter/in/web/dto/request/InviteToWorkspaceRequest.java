package com.tissue.workspace.adapter.in.web.dto.request;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.in.InviteToWorkspaceCommand;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record InviteToWorkspaceRequest(
        @NotEmpty Set<@Email @NotBlank String> emails,
        @NotNull WorkspaceRole role,
        Set<ProjectJoinConfigDto> targetProjects) {
    public InviteToWorkspaceCommand toCommand(String workspaceKey) {
        return new InviteToWorkspaceCommand(emails, workspaceKey, role, targetProjects);
    }
}
