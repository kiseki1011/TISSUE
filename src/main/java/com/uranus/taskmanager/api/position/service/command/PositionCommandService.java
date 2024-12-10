package com.uranus.taskmanager.api.position.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.position.domain.Position;
import com.uranus.taskmanager.api.position.domain.repository.PositionRepository;
import com.uranus.taskmanager.api.position.presentation.dto.request.CreatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.request.UpdatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.response.CreatePositionResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.DeletePositionResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.UpdatePositionResponse;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionCommandService {

	private final WorkspaceRepository workspaceRepository;
	private final PositionRepository positionRepository;

	@Transactional
	public CreatePositionResponse createPosition(String workspaceCode, CreatePositionRequest request) {
		// 워크스페이스 조회 및 검증
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		// Position 생성
		Position position = workspace.createPosition(request.name(), request.description());
		Position savedPosition = positionRepository.save(position);

		log.info("Position created: id={}, name={}, workspace={}",
			savedPosition.getId(), savedPosition.getName(), workspaceCode);

		return CreatePositionResponse.from(savedPosition);
	}

	@Transactional
	public UpdatePositionResponse updatePosition(String workspaceCode, Long positionId,
		UpdatePositionRequest request) {
		// Position 조회 및 검증
		Position position = positionRepository.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(() ->
				new RuntimeException("PositionNotFound: " + positionId)
			); // Todo: new PositionNotFoundException(positionId) 사용

		// Position 업데이트
		position.updateName(request.name());
		position.updateDescription(request.description());

		log.info("Position updated: id={}, name={}, workspace={}",
			position.getId(), position.getName(), workspaceCode);

		return UpdatePositionResponse.from(position);
	}

	@Transactional
	public DeletePositionResponse deletePosition(String workspaceCode, Long positionId) {
		// Position 조회 및 검증
		Position position = positionRepository.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(() ->
				new RuntimeException("PositionNotFound: " + positionId)
			); // Todo: new PositionNotFoundException(positionId) 사용

		// Position이 사용 중인지 확인
		if (positionRepository.existsByWorkspaceMembers(position)) {
			throw new RuntimeException("Cannot delete position that is in use"); // Todo: PositionInUseException 정의해서 사용
		}

		// Position 삭제
		positionRepository.delete(position);

		log.info("Position deleted: id={}, name={}, workspace={}",
			position.getId(), position.getName(), workspaceCode);

		return DeletePositionResponse.from(position);
	}
}
