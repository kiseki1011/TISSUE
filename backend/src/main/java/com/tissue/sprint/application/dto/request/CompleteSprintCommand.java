package com.tissue.sprint.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record CompleteSprintCommand(Long sprintId, ProjectMemberContext actorContext) {}
