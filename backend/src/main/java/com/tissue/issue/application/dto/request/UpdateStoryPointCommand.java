package com.tissue.issue.application.dto.request;

public record UpdateStoryPointCommand(
        String workspaceKey, String projectKey, String issueKey, Integer storyPoint, Long actorMemberId) {}
