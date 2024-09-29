package com.uranus.taskmanager.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;
import com.uranus.taskmanager.api.util.WorkspaceCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HandleDatabaseExceptionService와 다르게 WorkspaceCode의 중복 검사를 진행한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCodeDuplicationService implements WorkspaceCreateService {

	private static final int MAX_RETRIES = 5;

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;

	/**
	 * Todo: createWorkspace() 가독성 좋은 코드로 리팩토링
	 */
	@Override
	@Transactional
	public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {
		for (int count = 0; count < MAX_RETRIES; count++) {
			String workspaceCode = workspaceCodeGenerator.generateWorkspaceCode();
			if (workspaceCodeIsNotDuplicate(workspaceCode)) {
				log.info("[workspaceCodeIsNotDuplicate] workspaceCode = {}", workspaceCode);
				request.setWorkspaceCode(workspaceCode);
				Workspace workspace = workspaceRepository.save(request.toEntity());
				return WorkspaceResponse.fromEntity(workspace);
			}
			log.info("[Workspace Code Collision] Retrying... attempt {}", count + 1);
		}
		throw new RuntimeException(
			"Failed to solve workspace code collision"); // Todo: WorkspaceCode 재생성 반복 후에도 실패하는 경우 예외 처리
	}

	public boolean workspaceCodeIsNotDuplicate(String workspaceCode) {
		return !workspaceRepository.existsByWorkspaceCode(workspaceCode);
	}
}
