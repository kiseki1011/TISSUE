package com.tissue.issue.application.dto.request;

import lombok.Builder;

@Builder
public record RemoveIssueRelationCommand(
        String workspaceKey,
        String sourceProjectKey,
        String sourceIssueKey,
        String targetProjectKey,
        String targetIssueKey,
        Long actorMemberId) {}
