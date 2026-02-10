package com.tissue.feature.project.application.dto.request;

public record UpdateProjectKeyCommand(String workspaceKey, String projectKey, String newKey) {}
