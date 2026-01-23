package com.tissue.issue.application.dto.request;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record PerformSystemTransitionCommand(
        String issueKey,
        Long transitionId,
        String workspaceKey,
        String projectKey,
        @Nullable String vcsUserEmail,
        @Nullable String vcsUserName,
        @Nullable String triggerReason) {}
