package com.tissue.feature.sprint.application.dto.request;

import java.util.List;
import lombok.Builder;

@Builder
public record MigrateSprintIssuesCommand(Long targetSprintId, List<String> issueKeys) {}
