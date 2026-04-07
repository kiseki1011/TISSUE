package com.tissue.feature.project.application.dto.request;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateProjectCommand(
        String projectKey,
        String title,
        String description,
        @Nullable Long projectTemplateId) {}
