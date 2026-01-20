package com.tissue.issue.application.dto.request;

import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record AddIssueRelationCommand(
        String sourceIssueKey,
        String targetProjectKey,
        String targetIssueKey,
        IssueRelationType relationType,
        ProjectMemberContext actorContext) {}
