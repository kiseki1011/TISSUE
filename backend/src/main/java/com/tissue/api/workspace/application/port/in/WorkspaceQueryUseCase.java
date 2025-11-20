package com.tissue.api.workspace.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.response.WorkspaceDetail;

public interface WorkspaceQueryUseCase {

	// @PreAuthorize(REQUIRES_MEMBER)
	@Transactional(readOnly = true)
	WorkspaceDetail getDetail(String workspaceKey);
}
