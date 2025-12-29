package com.tissue.project.application.dto.request;

public record DirectJoinProjectCommand(String workspaceKey, String projectKey, Long actorMemberId) {}
