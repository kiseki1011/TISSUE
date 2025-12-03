package com.tissue.api.workspace.application.dto.request;

import java.util.Set;

import com.tissue.api.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

public record InviteToWorkspaceCommand(
	String workspaceKey,
	WorkspaceRole workspaceRole,
	Set<ProjectJoinConfigDto> targetProjects,
	Set<String> emails,
	Long actorMemberId
) {
}
