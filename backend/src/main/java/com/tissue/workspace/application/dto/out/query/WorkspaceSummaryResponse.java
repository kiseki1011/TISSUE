package com.tissue.workspace.application.dto.out.query;

import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record WorkspaceSummaryResponse(
    String workspaceKey, String name, @Nullable String description, Instant createdAt, WorkspaceRole myRole) {

    public static WorkspaceSummaryResponse from(WorkspaceMember workspaceMember) {
        Workspace workspace = workspaceMember.getWorkspace();
        return WorkspaceSummaryResponse.builder()
                .workspaceKey(workspace.getKey())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .createdAt(workspace.getCreatedAt())
                .myRole(workspaceMember.getRole())
                .build();
    }
}
