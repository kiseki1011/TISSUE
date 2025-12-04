package com.tissue.api.workspace.application.port.in;

import static com.tissue.api.security.authorization.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.request.DeleteWorkspaceCommand;
import com.tissue.api.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.api.workspace.application.dto.request.UpdateWorkspaceInfoCommand;

@Transactional
public interface WorkspaceCommandUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN)
	void updateInfo(UpdateWorkspaceInfoCommand cmd);

	@PreAuthorize(REQUIRES_WORKSPACE_OWNER)
	void delete(DeleteWorkspaceCommand cmd);

	@PreAuthorize(REQUIRES_WORKSPACE_OWNER)
	void transferOwnership(TransferOwnershipCommand cmd);

	// TODO: restoreWorkspace - softDeleted 상태를 복구
	// TODO: archiveWorkspace - 워크스페이스 아카이브(read-only로 변경). 하위 리소스 모두 archive 되어야 할까?
	// TODO: unarchiveWorkspace - 워크스페이스 아카이브 해제. 하위 리소스도 모두 unarchive.
}
