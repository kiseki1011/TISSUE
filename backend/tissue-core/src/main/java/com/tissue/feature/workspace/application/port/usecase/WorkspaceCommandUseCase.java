package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;

public interface WorkspaceCommandUseCase {

    WorkspaceCreateResponse create(CreateWorkspaceCommand cmd, Long actorMemberId);

    void update(String workspaceKey, UpdateWorkspaceInfoCommand cmd, Long actorMemberId);

    void delete(String workspaceKey, Long actorMemberId);

    void transferOwnership(String workspaceKey, Long targetMemberId, Long actorMemberId);

    void archive(String workspaceKey, Long actorMemberId);

    void restoreArchived(String workspaceKey, Long actorMemberId);

    void restoreDeleted(String workspaceKey, Long actorMemberId);
}
