package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import java.util.List;

public interface WorkspaceUseCase {

    WorkspaceCreateResponse create(CreateWorkspaceCommand cmd, Long memberId);

    void update(String workspaceKey, UpdateWorkspaceInfoCommand cmd, Long memberId);

    void transferOwnership(String workspaceKey, Long targetMemberId, Long memberId);

    void delete(String workspaceKey, Long memberId);

    WorkspaceDetail getDetail(String workspaceKey, Long memberId);

    List<WorkspaceSummaryResponse> getMyWorkspaces(Long memberId);

    // TODO: restoreDeletedWorkspace
    // TODO: archiveWorkspace
    // TODO: restoreArchivedWorkspace
}
