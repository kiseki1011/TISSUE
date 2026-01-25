package com.tissue.workspace.application.dto.out.query;

import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.Builder;

@Builder
public record WorkspaceSummaryResponse(String workspaceKey, String name, String description, WorkspaceRole myRole) {
    public static WorkspaceSummaryResponse from(WorkspaceMember workspaceMember) {
        Workspace workspace = workspaceMember.getWorkspace();
        return WorkspaceSummaryResponse.builder()
                .workspaceKey(workspace.getKey())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .myRole(workspaceMember.getRole())
                .build();
    }
}
