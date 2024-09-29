package com.uranus.taskmanager.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import com.uranus.taskmanager.api.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.response.WorkspaceResponse;

import lombok.RequiredArgsConstructor;

/**
 * Todo
 * UserWorkspaceService, AdminWorkspaceService, ReaderWorkspaceService 분리 고려
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private final WorkspaceRepository workspaceRepository;

	/**
	 * Todo
	 * 조회 로직 수정 필요
	 */
	@Transactional(readOnly = true)
	public WorkspaceResponse get(String workspaceCode) {
		Workspace workspace = workspaceRepository.findByWorkspaceCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
		return WorkspaceResponse.fromEntity(workspace);
	}
}
