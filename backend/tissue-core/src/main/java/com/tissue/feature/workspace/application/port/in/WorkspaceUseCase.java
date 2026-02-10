package com.tissue.feature.workspace.application.port.in;

import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import java.util.List;

public interface WorkspaceUseCase {

    WorkspaceCreateResponse create(CreateWorkspaceCommand cmd, Long memberId);

    void update(UpdateWorkspaceInfoCommand cmd, WorkspaceMemberContext actorContext);

    void transferOwnership(Long targetMemberId, WorkspaceMemberContext actorContext);

    void delete(WorkspaceMemberContext actorContext);

    WorkspaceDetail getDetail(WorkspaceMemberContext actorContext);

    List<WorkspaceSummaryResponse> getMyWorkspaces(Long memberId);

    // TODO: restoreDeletedWorkspace
    // TODO: archiveWorkspace
    // TODO: restoreArchivedWorkspace
}
