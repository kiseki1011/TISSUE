package com.tissue.workspace.application.dto.out.query;

import com.tissue.workspace.domain.Workspace;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record WorkspaceDetail(
        Long id,
        String key,
        String name,
        @Nullable String description,
        // int memberCount,
        Long createdBy,
        Instant createdAt,
        Long updatedBy,
        Instant updatedAt) {

    public static WorkspaceDetail from(Workspace workspace) {
        return WorkspaceDetail.builder()
                .id(workspace.getId())
                .key(workspace.getKey())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .createdBy(workspace.getCreatedBy())
                .createdAt(workspace.getCreatedAt())
                .updatedBy(workspace.getLastModifiedBy())
                .updatedAt(workspace.getLastModifiedAt())
                .build();
    }
}
