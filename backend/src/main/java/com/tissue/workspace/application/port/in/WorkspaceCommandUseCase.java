package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.UpdateWorkspaceInfoCommand;

public interface WorkspaceCommandUseCase {

    void updateInfo(UpdateWorkspaceInfoCommand cmd, WorkspaceMemberContext actorContext);

    void transferOwnership(Long targetMemberId, WorkspaceMemberContext actorContext);

    void delete(WorkspaceMemberContext actorContext);

    // TODO: restoreDeletedWorkspace
    // TODO: archiveWorkspace
    // TODO: restoreArchivedWorkspace
}
