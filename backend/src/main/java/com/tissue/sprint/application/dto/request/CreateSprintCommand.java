package com.tissue.sprint.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record CreateSprintCommand(String title, String goal, ProjectMemberContext actorContext) {}
