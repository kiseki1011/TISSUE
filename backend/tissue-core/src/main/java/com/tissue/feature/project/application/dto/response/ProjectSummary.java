package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectVisibility;
import java.time.Instant;

public record ProjectSummary(
        String key,
        String title,
        String description,
        ProjectVisibility visibility,
        boolean archived,
        Instant createdAt,
        Instant lastUpdatedAt) {

    public static ProjectSummary from(Project project) {
        return new ProjectSummary(
                project.getKey(),
                project.getTitle(),
                project.getDescription(),
                project.getVisibility(),
                project.isArchived(),
                project.getCreatedAt(),
                project.getLastModifiedAt());
    }
}
