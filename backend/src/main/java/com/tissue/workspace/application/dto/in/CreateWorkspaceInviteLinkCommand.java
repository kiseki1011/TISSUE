package com.tissue.workspace.application.dto.in;

import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.domain.enums.WorkspaceRole;

import lombok.Builder;

@Builder
public record CreateWorkspaceInviteLinkCommand(
	String workspaceKey,
	WorkspaceRole workspaceRole,
	List<ProjectJoinConfigDto> targetProjects,
	@Nullable Instant expiredAt
) {
}
