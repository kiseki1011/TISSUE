package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.workspace.application.dto.request.UpdateWorkspaceInfoCommand;

public interface WorkspaceCommandUseCase {

    void updateInfo(UpdateWorkspaceInfoCommand cmd);

    void transferOwnership(TransferOwnershipCommand cmd);

    void delete(WorkspaceMemberContext actorContext);

    // TODO: restoreDeletedWorkspace - softDeleted 상태를 복구
    // TODO: archiveWorkspace - 워크스페이스 아카이브(read-only로 변경)
    // TODO: restoreArchivedWorkspace - 워크스페이스 아카이브 해제
}
