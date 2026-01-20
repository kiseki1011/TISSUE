package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record PerformTransitionCommand(String issueKey, Long transitionId, ProjectMemberContext actorContext) {}
