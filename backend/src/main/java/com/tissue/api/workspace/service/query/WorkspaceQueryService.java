package com.tissue.api.workspace.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceQueryRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceQueryService {

	private final WorkspaceQueryRepository workspaceQueryRepository;

	@Transactional(readOnly = true)
	public WorkspaceDetail getWorkspaceDetail(String workspaceCode) {

		Workspace workspace = workspaceQueryRepository.findByCode(workspaceCode)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceCode));

		return WorkspaceDetail.from(workspace);
	}

}
