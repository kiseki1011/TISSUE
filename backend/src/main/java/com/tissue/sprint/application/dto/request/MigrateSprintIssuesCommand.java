package com.tissue.sprint.application.dto.request;

import java.util.List;
import lombok.Builder;

@Builder
public record MigrateSprintIssuesCommand(
        String workspaceKey, String projectKey, Long originalSprintId, Long newSprintId, List<String> issueKeys) {}
