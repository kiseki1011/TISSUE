package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record UnsubscribeIssueCommand(String issueKey, ProjectMemberContext actor) {}
