package com.tissue.issue.application.dto.request;

public record SubmitReviewCommand(
        String workspaceKey,
        String projectKey,
        String issueKey,
        boolean approved,
        Long actorMemberId) {}
