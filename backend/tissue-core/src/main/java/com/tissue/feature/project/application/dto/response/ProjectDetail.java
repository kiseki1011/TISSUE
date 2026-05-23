package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectVisibility;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record ProjectDetail(
        String key,
        String title,
        String description,
        ProjectVisibility visibility,
        boolean archived,
        @Nullable Long createdBy,
        @Nullable Long lastModifiedBy,
        Instant createdAt,
        Instant lastUpdatedAt) {

    public static ProjectDetail from(Project project) {
        return new ProjectDetail(
                project.getKey(),
                project.getTitle(),
                project.getDescription(),
                project.getVisibility(),
                project.isArchived(),
                project.getCreatedBy(),
                project.getLastModifiedBy(),
                project.getCreatedAt(),
                project.getLastModifiedAt());
    }
}
