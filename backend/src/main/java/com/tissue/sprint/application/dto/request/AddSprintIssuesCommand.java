package com.tissue.sprint.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;

public record AddSprintIssuesCommand(Long sprintId, List<String> issueKeys, ProjectMemberContext actorContext) {}
