package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record DeleteIssueCommand(String issueKey, ProjectMemberContext actorContext) {}
