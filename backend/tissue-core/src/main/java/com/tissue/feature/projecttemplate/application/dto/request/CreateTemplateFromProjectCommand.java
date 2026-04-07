package com.tissue.feature.projecttemplate.application.dto.request;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateTemplateFromProjectCommand(
        String workspaceKey,
        String projectKey,
        String name,
        @Nullable String description) {}
