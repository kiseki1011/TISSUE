package com.uranus.taskmanager.api.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
 * DB에서 발생하는 예외를 서비스 계층에서 잡아서 핸들링 로직(워크스페이스 코드 재생성) 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandleDatabaseExceptionService implements WorkspaceCreateService {
	private static final int MAX_RETRIES = 5;

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;

	/**
	 * Todo: createWorkspace() 가독성 좋은 코드로 리팩토링 필요
	 */
	@Override
	@Transactional
	public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {

		for (int count = 0; count < MAX_RETRIES; count++) {
			try {
				String workspaceCode = workspaceCodeGenerator.generateWorkspaceCode();
				if (count != 0) {
					workspaceCode = workspaceCodeGenerator.generateWorkspaceCode();
					log.info("[Recreate Workspace Code] workspaceCode = {}", workspaceCode);
				}
				request.setWorkspaceCode(workspaceCode);

				Workspace workspace = workspaceRepository.saveWithNewTransaction(request.toEntity());
				return WorkspaceResponse.fromEntity(workspace);
			} catch (DataIntegrityViolationException | ConstraintViolationException e) {
				/*
				 * Todo: 로그 정리
				 */
				log.error("[Catched Exception for Workspace Code Collision]", e);
				log.info("[Workspace Code Collision] Retrying... attempt {}", count + 1);
			}
		}
		throw new RuntimeException(
			"Failed to solve workspace code collision"); // Todo: WorkspaceCode 재생성 반복 후에도 실패하는 경우 예외 처리
	}
}
