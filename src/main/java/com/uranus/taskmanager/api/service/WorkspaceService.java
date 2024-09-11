package com.uranus.taskmanager.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private final WorkspaceRepository workspaceRepository;

	@Transactional
	public WorkspaceResponse create(WorkspaceCreateRequest request) {
		Workspace workspace = workspaceRepository.save(request.toEntity());
		workspace.setWorkspaceId(UUID.randomUUID().toString());
		log.info("[WorkspaceService.create] workspace = {}", workspace);
		return WorkspaceResponse.fromEntity(workspace);
	}

	@Transactional(readOnly = true)
	public WorkspaceResponse get(String workspaceId) {
		Workspace workspace = workspaceRepository.findByWorkspaceId(workspaceId)
			// RuntimeException -> WorkspaceNotFoundException 정의 예정
			.orElseThrow(() -> new RuntimeException("Workspace was not found!"));
		return WorkspaceResponse.fromEntity(workspace);
	}

}
