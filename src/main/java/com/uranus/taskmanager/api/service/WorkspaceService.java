package com.uranus.taskmanager.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import com.uranus.taskmanager.api.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

	/**
	 * Todo
	 * UserWorkspaceService, AdminWorkspaceService, ReaderWorkspaceService 분리 고려
	 */

	private final WorkspaceRepository workspaceRepository;

	@Transactional
	public WorkspaceResponse create(WorkspaceCreateRequest request) {
		Workspace workspace = workspaceRepository.save(request.toEntity());
		workspace.setWorkspaceCode(UUID.randomUUID().toString());
		return WorkspaceResponse.fromEntity(workspace);
	}

	@Transactional(readOnly = true)
	public WorkspaceResponse get(String workspaceCode) {
		Workspace workspace = workspaceRepository.findByWorkspaceCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
		return WorkspaceResponse.fromEntity(workspace);
	}

}
