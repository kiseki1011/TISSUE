package com.tissue.feature.projecttemplate.application.dto.response;

import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import java.time.Instant;

public record ProjectTemplateSummary(Long id, String name, String description, Instant createdAt) {

    public static ProjectTemplateSummary from(ProjectTemplate template) {
        return new ProjectTemplateSummary(
                template.getId(), template.getName(), template.getDescription(), template.getCreatedAt());
    }
}
