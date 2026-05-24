package com.tissue.feature.projecttemplate.application.dto.response;

import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import com.tissue.feature.projecttemplate.domain.config.TemplateConfig;
import java.time.Instant;

public record ProjectTemplateDetail(
        Long id, String workspaceKey, String name, String description, Instant createdAt, TemplateConfig config) {

    public static ProjectTemplateDetail from(ProjectTemplate template) {
        return new ProjectTemplateDetail(
                template.getId(),
                template.getWorkspace().getKey(),
                template.getName(),
                template.getDescription(),
                template.getCreatedAt(),
                template.getConfigPayload());
    }
}
