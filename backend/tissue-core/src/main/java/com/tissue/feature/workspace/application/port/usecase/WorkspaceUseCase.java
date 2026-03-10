package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.dto.response.query.DeletedWorkspaceSummary;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import java.util.List;

public interface WorkspaceUseCase {

    WorkspaceCreateResponse create(CreateWorkspaceCommand cmd, Long actorMemberId);

    void update(String workspaceKey, UpdateWorkspaceInfoCommand cmd, Long actorMemberId);

    void transferOwnership(String workspaceKey, Long targetMemberId, Long actorMemberId);

    void delete(String workspaceKey, Long actorMemberId);

    WorkspaceDetail getDetail(String workspaceKey, Long actorMemberId);

    List<WorkspaceSummaryResponse> getMyWorkspaces(Long actorMemberId);

    void archive(String workspaceKey, Long actorMemberId);

    void restoreArchived(String workspaceKey, Long actorMemberId);

    void restoreDeleted(String workspaceKey, Long actorMemberId);

    List<DeletedWorkspaceSummary> getMyDeletedWorkspaces(Long actorMemberId);
}
