package com.tissue.position.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.common.enums.ColorType;
import com.tissue.position.domain.model.Position;
import com.tissue.position.infrastructure.repository.PositionRepository;
import com.tissue.position.presentation.dto.request.CreatePositionRequest;
import com.tissue.position.presentation.dto.request.UpdatePositionColorRequest;
import com.tissue.position.presentation.dto.request.UpdatePositionRequest;
import com.tissue.position.presentation.dto.response.PositionResponse;
import com.tissue.position.validator.PositionValidator;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionCommandService {

	private final PositionFinder positionFinder;
	private final WorkspaceFinder workspaceFinder;
	private final PositionRepository positionRepository;
	private final PositionValidator positionValidator;

	// TODO: refactor so the user can set the color
	@Transactional
	public PositionResponse createPosition(
		String workspaceCode,
		CreatePositionRequest request
	) {
		Workspace workspace = workspaceFinder.getModifiableBy(workspaceCode);

		Position position = Position.builder()
			.name(request.name())
			.description(request.description())
			.color(ColorType.getRandomColor())
			.workspace(workspace)
			.build();

		return PositionResponse.from(positionRepository.save(position));
	}

	// TODO: refactor so the user can set the color
	// TODO: refactor to use Patchers.apply
	@Transactional
	public PositionResponse updatePosition(
		String workspaceCode,
		Long positionId,
		UpdatePositionRequest request
	) {
		Position position = positionFinder.findByIdAndWorkspaceKey(positionId, workspaceCode);

		position.updateName(request.name());
		position.updateDescription(request.description());

		return PositionResponse.from(position);
	}

	// TODO: remove after refactoring createPosition()
	@Transactional
	public PositionResponse updatePositionColor(
		String workspaceCode,
		Long positionId,
		UpdatePositionColorRequest request
	) {
		Position position = positionFinder.findByIdAndWorkspaceKey(positionId, workspaceCode);

		position.updateColor(request.colorType());

		return PositionResponse.from(position);
	}

	@Transactional
	public void deletePosition(
		String workspaceCode,
		Long positionId
	) {
		Position position = positionFinder.findByIdAndWorkspaceKey(positionId, workspaceCode);

		positionValidator.ensureDeletable(position);

		positionRepository.delete(position);
	}
}
