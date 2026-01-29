package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.workspace.application.dto.request.UpdateWorkspaceInfoCommand;

public interface WorkspaceCommandUseCase {

    void updateInfo(UpdateWorkspaceInfoCommand cmd);

    void transferOwnership(TransferOwnershipCommand cmd);

    void delete(WorkspaceMemberContext actorContext);

    // TODO: restoreDeletedWorkspace
    // TODO: archiveWorkspace
    // TODO: restoreArchivedWorkspace
}
