package com.tissue.workspace.application.dto.in;

import java.util.Set;

import com.tissue.project.domain.enums.ProjectRole;

public record InviteToProjectCommand(
	Set<String> emails,
	String workspaceKey,
	String projectKey,
	ProjectRole role
) {
}
