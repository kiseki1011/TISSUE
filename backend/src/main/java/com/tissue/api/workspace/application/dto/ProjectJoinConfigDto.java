package com.tissue.api.workspace.application.dto;

import com.tissue.api.project.domain.enums.ProjectRole;

public record ProjectJoinConfigDto(
	String projectKey,
	ProjectRole role
) {
}
