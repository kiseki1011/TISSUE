package com.tissue.api.workspace.adapter.in.web.dto.request;

import java.util.Set;

import com.tissue.api.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.api.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record InviteToWorkspaceRequest(
	@NotEmpty Set<@Email @NotBlank String> emails,
	@NotNull WorkspaceRole role,
	Set<ProjectJoinConfigDto> targetProjects
) {
	public InviteToWorkspaceCommand toCommand(String workspaceKey) {
		return new InviteToWorkspaceCommand(
			emails,
			workspaceKey,
			role,
			targetProjects
		);
	}
}
