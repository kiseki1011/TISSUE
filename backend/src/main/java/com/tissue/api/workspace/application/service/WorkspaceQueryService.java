package com.tissue.api.workspace.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.domain.port.out.WorkspaceQueryRepository;
import com.tissue.api.workspace.adapter.in.web.dto.WorkspaceDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceQueryService {

	private final WorkspaceQueryRepository workspaceQueryRepository;

	@Transactional(readOnly = true)
	public WorkspaceDetail getWorkspaceDetail(String workspaceCode) {

		Workspace workspace = workspaceQueryRepository.findByKey(workspaceCode)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceCode));

		return WorkspaceDetail.from(workspace);
	}

}
