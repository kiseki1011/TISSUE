package com.tissue.sprint.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;
import lombok.Builder;

@Builder
public record MigrateSprintIssuesCommand(
        Long originalSprintId, Long newSprintId, List<String> issueKeys, ProjectMemberContext actorContext) {}
