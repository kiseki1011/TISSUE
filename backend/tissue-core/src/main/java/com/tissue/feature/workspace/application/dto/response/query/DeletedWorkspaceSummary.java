package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.workspace.domain.Workspace;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record DeletedWorkspaceSummary(
        String workspaceKey,
        String name,
        String description,
        @Nullable Instant deletedAt) {

    public static DeletedWorkspaceSummary from(Workspace workspace) {
        return new DeletedWorkspaceSummary(
                workspace.getKey(), workspace.getName(), workspace.getDescription(), workspace.getSoftDeletedAt());
    }
}
