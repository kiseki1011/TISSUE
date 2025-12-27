package com.tissue.workspace.application.port.in;

import static com.tissue.workspace.application.service.authorization.WorkspaceAuthExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.workspace.application.dto.in.DeleteWorkspaceCommand;
import com.tissue.workspace.application.dto.in.TransferOwnershipCommand;
import com.tissue.workspace.application.dto.in.UpdateWorkspaceInfoCommand;

public interface WorkspaceCommandUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN)
	void updateInfo(UpdateWorkspaceInfoCommand cmd);

	@PreAuthorize(REQUIRES_WORKSPACE_OWNER)
	void delete(DeleteWorkspaceCommand cmd);

	@PreAuthorize(REQUIRES_WORKSPACE_OWNER)
	void transferOwnership(TransferOwnershipCommand cmd);

	// TODO: restoreDeletedWorkspace - softDeleted 상태를 복구
	// TODO: archiveWorkspace - 워크스페이스 아카이브(read-only로 변경)
	// TODO: restoreArchivedWorkspace - 워크스페이스 아카이브 해제
}
