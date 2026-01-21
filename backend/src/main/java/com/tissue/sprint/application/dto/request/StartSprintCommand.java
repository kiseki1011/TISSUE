package com.tissue.sprint.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.time.Instant;
import lombok.Builder;

@Builder
public record StartSprintCommand(Long sprintId, Instant dueAt, ProjectMemberContext actorContext) {}
