package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record AssignParentCommand(
        String issueKey, String parentProjectKey, String parentIssueKey, ProjectMemberContext actorContext) {}
