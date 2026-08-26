package com.tissue.feature.sprint.application.dto.request;

import lombok.Builder;

@Builder
public record MigrateSprintIssuesCommand(Long targetSprintId) {}
