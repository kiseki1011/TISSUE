package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import org.jspecify.annotations.Nullable;

public record UpdateStoryPointCommand(
        String issueKey, @Nullable Integer storyPoint, ProjectMemberContext actorContext) {}
