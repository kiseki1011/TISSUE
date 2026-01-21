package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record RemoveParentCommand(String issueKey, ProjectMemberContext actorContext) {}
