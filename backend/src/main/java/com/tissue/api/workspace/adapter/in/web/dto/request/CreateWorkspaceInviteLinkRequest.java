package com.tissue.api.workspace.adapter.in.web.dto.request;

import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.api.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record CreateWorkspaceInviteLinkRequest(
	@NotNull WorkspaceRole workspaceRole,
	@Nullable List<ProjectJoinConfigDto> targetProjects,
	@Nullable @Future Instant expiredAt
) {
	public CreateWorkspaceInviteLinkCommand toCommand(String workspaceKey) {
		return new CreateWorkspaceInviteLinkCommand(
			workspaceKey,
			workspaceRole,
			targetProjects,
			expiredAt
		);
	}
}
