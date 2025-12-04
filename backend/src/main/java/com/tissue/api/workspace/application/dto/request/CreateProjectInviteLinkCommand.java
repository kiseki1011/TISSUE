package com.tissue.api.workspace.application.dto.request;

import java.time.Instant;

import org.springframework.lang.Nullable;

import com.tissue.api.project.domain.enums.ProjectRole;

import lombok.Builder;

@Builder
public record CreateProjectInviteLinkCommand(
	String workspaceKey,
	String projectKey,
	ProjectRole role,
	@Nullable Instant expiredAt
) {
}
