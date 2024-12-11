package com.uranus.taskmanager.api.position.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.position.domain.Position;
import com.uranus.taskmanager.api.position.domain.repository.PositionRepository;
import com.uranus.taskmanager.api.position.exception.PositionNotFoundException;
import com.uranus.taskmanager.api.position.presentation.dto.request.CreatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.request.UpdatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.response.CreatePositionResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.DeletePositionResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.UpdatePositionResponse;
import com.uranus.taskmanager.api.position.validator.PositionValidator;
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
	private final PositionValidator positionValidator;

	@Transactional
	public CreatePositionResponse createPosition(
		String workspaceCode,
		CreatePositionRequest request
	) {

		Workspace workspace = findWorkspaceByCode(workspaceCode);

		positionValidator.validateDuplicatePositionName(
			workspaceCode,
			request.name()
		);

		Position savedPosition = createPosition(
			request,
			workspace
		);

		return CreatePositionResponse.from(savedPosition);
	}

	@Transactional
	public UpdatePositionResponse updatePosition(
		String workspaceCode,
		Long positionId,
		UpdatePositionRequest request
	) {

		positionValidator.validateDuplicatePositionName(
			workspaceCode,
			request.name()
		);

		Position position = findPositionByIdAndWorkspaceCode(
			workspaceCode,
			positionId
		);

		position.updateName(request.name());
		position.updateDescription(request.description());

		return UpdatePositionResponse.from(position);
	}

	@Transactional
	public DeletePositionResponse deletePosition(
		String workspaceCode,
		Long positionId
	) {

		Position position = findPositionByIdAndWorkspaceCode(
			workspaceCode,
			positionId
		);

		positionValidator.validatePositionIsUsed(position);

		positionRepository.delete(position);

		return DeletePositionResponse.from(position);
	}

	private Position createPosition(CreatePositionRequest request, Workspace workspace) {
		Position position = workspace.createPosition(request.name(), request.description());
		return positionRepository.save(position);
	}

	private Workspace findWorkspaceByCode(String workspaceCode) {
		return workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
	}

	private Position findPositionByIdAndWorkspaceCode(String workspaceCode, Long positionId) {
		return positionRepository.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(PositionNotFoundException::new);
	}
}
