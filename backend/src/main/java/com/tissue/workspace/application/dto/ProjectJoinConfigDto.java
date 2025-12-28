package com.tissue.workspace.application.dto;

import com.tissue.project.domain.enums.ProjectRole;

public record ProjectJoinConfigDto(String projectKey, ProjectRole role) {}
