package com.tissue.api.workspace.application.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.response.WorkspaceDetail;
import com.tissue.api.workspace.application.port.in.WorkspaceQueryUseCase;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.domain.port.out.WorkspaceQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceQueryService implements WorkspaceQueryUseCase {

	private final WorkspaceQueryRepository workspaceQueryRepository;

	@Transactional(readOnly = true)
	public WorkspaceDetail getDetail(String workspaceKey) {

		Workspace workspace = workspaceQueryRepository.findByKey(workspaceKey)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));

		return WorkspaceDetail.from(workspace);
	}
}
