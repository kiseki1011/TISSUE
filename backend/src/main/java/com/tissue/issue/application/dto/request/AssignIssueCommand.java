package com.tissue.issue.application.dto.request;

import lombok.Builder;

@Builder
public record AssignIssueCommand(
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long targetMemberId,
        Long actorMemberId) {}
