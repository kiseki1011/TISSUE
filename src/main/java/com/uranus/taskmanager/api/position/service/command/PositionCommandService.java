package com.uranus.taskmanager.api.position.service.command;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.common.ColorPalette;
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

		// 현재 워크스페이스에서 사용 중인 색상들을 Set으로 추출
		Set<ColorPalette> usedColors = workspace.getPositions().stream()
			.map(Position::getColor)
			.collect(Collectors.toSet());

		// 사용되지 않은 색상 중에서 랜덤으로 선택
		ColorPalette randomColor = ColorPalette.getRandomUnusedColor(usedColors);

		Position savedPosition = createPosition(
			request,
			workspace,
			randomColor
		);

		log.debug("Position {} created with color: {} ({})",
			request.name(),
			randomColor.getDisplayName(),
			randomColor.getHexCode()
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

	private Position createPosition(CreatePositionRequest request, Workspace workspace, ColorPalette color) {
		Position position = workspace.createPosition(request.name(), request.description(), color);
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
