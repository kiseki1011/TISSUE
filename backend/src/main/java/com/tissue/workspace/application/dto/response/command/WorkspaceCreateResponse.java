package com.tissue.workspace.application.dto.response.command;

import com.tissue.workspace.domain.Workspace;

public record WorkspaceCreateResponse(String workspaceKey) {
    public static WorkspaceCreateResponse from(Workspace workspace) {
        return new WorkspaceCreateResponse(workspace.getKey());
    }
}
