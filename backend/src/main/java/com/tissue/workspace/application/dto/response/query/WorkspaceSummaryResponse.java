package com.tissue.workspace.application.dto.response.query;

import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder
public record WorkspaceSummaryResponse(
        String workspaceKey, String name, String description, Instant createdAt, WorkspaceRole myRole) {

    public static WorkspaceSummaryResponse from(WorkspaceMember workspaceMember) {
        Workspace workspace = workspaceMember.getWorkspace();
        return WorkspaceSummaryResponse.builder()
                .workspaceKey(workspace.getKey())
                .name(workspace.getName())
                .description(Objects.requireNonNullElse(workspace.getDescription(), ""))
                .createdAt(workspace.getCreatedAt())
                .myRole(workspaceMember.getRole())
                .build();
    }
}
