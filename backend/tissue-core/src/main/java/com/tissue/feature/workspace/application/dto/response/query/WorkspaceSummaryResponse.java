package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.enums.WorkspaceStatus;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder
public record WorkspaceSummaryResponse(
        String workspaceKey,
        String name,
        String description,
        Instant createdAt,
        WorkspaceRole myRole,
        WorkspaceStatus status,
        long memberCount) {

    public static WorkspaceSummaryResponse from(WorkspaceMember workspaceMember, long memberCount) {
        Workspace workspace = workspaceMember.getWorkspace();
        return WorkspaceSummaryResponse.builder()
                .workspaceKey(workspace.getKey())
                .name(workspace.getName())
                .description(Objects.requireNonNullElse(workspace.getDescription(), ""))
                .createdAt(workspace.getCreatedAt())
                .myRole(workspaceMember.getRole())
                .status(workspace.getStatus())
                .memberCount(memberCount)
                .build();
    }
}
