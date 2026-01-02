package com.tissue.issue.application.dto.request;

import org.jspecify.annotations.Nullable;

public record UpdateStoryPointCommand(
        String workspaceKey,
        String projectKey,
        String issueKey,
        @Nullable Integer storyPoint) {}
