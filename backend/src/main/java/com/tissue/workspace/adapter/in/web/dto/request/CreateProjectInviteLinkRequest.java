package com.tissue.workspace.adapter.in.web.dto.request;

import java.time.Instant;

import org.springframework.lang.Nullable;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record CreateProjectInviteLinkRequest(
	@NotNull ProjectRole role,
	@Nullable @Future Instant expiredAt
) {
	public CreateProjectInviteLinkCommand toCommand(String workspaceKey, String projectKey) {
		return new CreateProjectInviteLinkCommand(
			workspaceKey,
			projectKey,
			role,
			expiredAt
		);
	}
}
