package com.tissue.issue.application.dto.request;

import com.tissue.issue.domain.enums.IssueRelationType;
import lombok.Builder;

@Builder
public record AddIssueRelationCommand(
        String workspaceKey,
        String sourceProjectKey,
        String sourceIssueKey,
        String targetProjectKey,
        String targetIssueKey,
        IssueRelationType relationType,
        Long actorMemberId) {}
