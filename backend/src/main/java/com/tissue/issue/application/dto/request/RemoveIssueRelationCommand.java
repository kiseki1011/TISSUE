package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record RemoveIssueRelationCommand(
        String sourceIssueKey, String targetProjectKey, String targetIssueKey, ProjectMemberContext actorContext) {}
