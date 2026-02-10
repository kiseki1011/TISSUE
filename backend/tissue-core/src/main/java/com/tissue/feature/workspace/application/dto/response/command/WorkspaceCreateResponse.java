package com.tissue.feature.workspace.application.dto.response.command;

import com.tissue.feature.workspace.domain.Workspace;

public record WorkspaceCreateResponse(String workspaceKey) {
    public static WorkspaceCreateResponse from(Workspace workspace) {
        return new WorkspaceCreateResponse(workspace.getKey());
    }
}
