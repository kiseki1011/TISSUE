package com.tissue.api.project.application.dto.request;

import com.tissue.api.project.domain.enums.ProjectRole;

import lombok.Builder;

@Builder
public record ChangeProjectRoleCommand(
	String workspaceKey,
	String projectKey,
	Long targetMemberId,
	ProjectRole newRole,
	Long actorMemberId
) {
}
