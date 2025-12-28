package com.tissue.project.application.dto.request;

import lombok.Builder;

@Builder
public record CreateProjectCommand(
        String workspaceKey, String projectKey, String title, String description) {}
