package com.tissue.issue.application.dto.request;

import lombok.Builder;

@Builder
public record SubmitReviewCommand(
        String workspaceKey, String projectKey, String issueKey, boolean approved, Long actorMemberId) {}
