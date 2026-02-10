package com.tissue.feature.project.application.dto.request;

public record LeaveProjectCommand(String workspaceKey, String projectKey, Long actorMemberId) {}
