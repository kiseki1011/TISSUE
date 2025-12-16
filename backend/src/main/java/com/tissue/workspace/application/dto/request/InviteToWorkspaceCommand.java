package com.tissue.workspace.application.dto.request;

import java.util.Set;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public record InviteToWorkspaceCommand(
	Set<String> emails,
	String workspaceKey,
	WorkspaceRole role,
	Set<ProjectJoinConfigDto> targetProjects
) {
}
