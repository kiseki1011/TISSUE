package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.ProjectVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record ProjectSummary(
        String key,
        String title,
        String description,
        ProjectVisibility visibility,
        boolean archived,
        Instant createdAt,
        Instant lastUpdatedAt,

        @Schema(description = "Latest issue activity Instant, `null` when the project has no issue" + " activity yet")
        @Nullable
        Instant lastActivityAt,

        @Schema(description = "Number of active members in the project")
        long memberCount,

        @Schema(description = "The caller's role in the project, `null` when the caller is not a member") @Nullable
        ProjectRole myRole) {

    public static ProjectSummary from(
            Project project, @Nullable Instant lastActivityAt, long memberCount, @Nullable ProjectRole myRole) {
        return new ProjectSummary(
                project.getKey(),
                project.getTitle(),
                project.getDescription(),
                project.getVisibility(),
                project.isArchived(),
                project.getCreatedAt(),
                project.getLastModifiedAt(),
                lastActivityAt,
                memberCount,
                myRole);
    }
}
