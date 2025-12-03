package com.tissue.api.workspace.application.dto.request;

import java.util.Set;

import com.tissue.api.project.domain.enums.ProjectRole;

import lombok.Builder;

@Builder
public record InviteToProjectCommand(
	String workspaceKey,
	String projectKey,
	ProjectRole role,
	Set<String> emails,
	Long actorMemberId
) {
}
