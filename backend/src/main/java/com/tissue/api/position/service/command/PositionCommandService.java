package com.tissue.api.position.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.position.domain.Position;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.exception.PositionNotFoundException;
import com.tissue.api.position.presentation.dto.request.CreatePositionRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionColorRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionRequest;
import com.tissue.api.position.presentation.dto.response.CreatePositionResponse;
import com.tissue.api.position.presentation.dto.response.DeletePositionResponse;
import com.tissue.api.position.presentation.dto.response.UpdatePositionColorResponse;
import com.tissue.api.position.presentation.dto.response.UpdatePositionResponse;
import com.tissue.api.position.validator.PositionValidator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;

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

		ColorType randomColor = ColorType.getRandomUnusedColor(workspace.getUsedPositionColors());

		Position savedPosition = createPosition(request, workspace, randomColor);

		return CreatePositionResponse.from(savedPosition);
	}

	@Transactional
	public UpdatePositionResponse updatePosition(
		String workspaceCode,
		Long positionId,
		UpdatePositionRequest request
	) {
		Position position = findPosition(workspaceCode, positionId);

		position.updateName(request.name());
		position.updateDescription(request.description());

		return UpdatePositionResponse.from(position);
	}

	@Transactional
	public UpdatePositionColorResponse updatePositionColor(
		String workspaceCode,
		Long positionId,
		UpdatePositionColorRequest request
	) {
		Position position = findPosition(
			workspaceCode,
			positionId
		);

		position.updateColor(request.colorType());

		return UpdatePositionColorResponse.from(position);
	}

	@Transactional
	public DeletePositionResponse deletePosition(
		String workspaceCode,
		Long positionId
	) {

		Position position = findPosition(
			workspaceCode,
			positionId
		);

		positionValidator.validatePositionIsUsed(position);

		positionRepository.delete(position);

		return DeletePositionResponse.from(position);
	}

	private Position createPosition(
		CreatePositionRequest request,
		Workspace workspace,
		ColorType color
	) {
		Position position = Position.builder()
			.name(request.name())
			.description(request.description())
			.color(color)
			.workspace(workspace)
			.build();

		return positionRepository.save(position);
	}

	private Workspace findWorkspaceByCode(String workspaceCode) {
		return workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
	}

	private Position findPosition(
		String workspaceCode,
		Long positionId
	) {
		return positionRepository.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(PositionNotFoundException::new);
	}
}
