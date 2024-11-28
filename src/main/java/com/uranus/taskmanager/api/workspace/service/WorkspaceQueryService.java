package com.uranus.taskmanager.api.workspace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceQueryService {

	private final WorkspaceRepository workspaceRepository;

	@Transactional(readOnly = true)
	public WorkspaceDetail getWorkspaceDetail(String workspaceCode) {

		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		return WorkspaceDetail.from(workspace);
	}
}
