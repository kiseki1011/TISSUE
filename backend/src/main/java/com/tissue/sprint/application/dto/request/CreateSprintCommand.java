package com.tissue.sprint.application.dto.request;

import lombok.Builder;

@Builder
public record CreateSprintCommand(String workspaceKey, String projectKey, String title, String goal) {}
