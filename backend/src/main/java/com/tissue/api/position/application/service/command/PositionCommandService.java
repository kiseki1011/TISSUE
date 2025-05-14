package com.tissue.api.position.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.position.infrastructure.repository.PositionRepository;
import com.tissue.api.position.presentation.dto.request.CreatePositionRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionColorRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionRequest;
import com.tissue.api.position.presentation.dto.response.PositionResponse;
import com.tissue.api.position.validator.PositionValidator;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionCommandService {

	private final PositionReader positionReader;
	private final WorkspaceReader workspaceReader;
	private final PositionRepository positionRepository;
	private final PositionValidator positionValidator;

	// TODO: 포지션 생성 시, 색을 정할 수 있도록 설정
	@Transactional
	public PositionResponse createPosition(
		String workspaceCode,
		CreatePositionRequest request
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		Position position = Position.builder()
			.name(request.name())
			.description(request.description())
			.color(ColorType.getRandomColor())
			.workspace(workspace)
			.build();

		return PositionResponse.from(positionRepository.save(position));
	}

	// TODO: 포지션 업데이트 시, 색을 정할 수 있도록 설정(null인 필드는 업데이트 하지 않는 방식으로 진행)
	@Transactional
	public PositionResponse updatePosition(
		String workspaceCode,
		Long positionId,
		UpdatePositionRequest request
	) {
		Position position = positionReader.findPosition(positionId, workspaceCode);

		position.updateName(request.name());
		position.updateDescription(request.description());

		return PositionResponse.from(position);
	}

	// TODO: 색 업데이트를 updatePosition에 포함할 시, 이 메서드와 API 삭제
	@Transactional
	public PositionResponse updatePositionColor(
		String workspaceCode,
		Long positionId,
		UpdatePositionColorRequest request
	) {
		Position position = positionReader.findPosition(positionId, workspaceCode);

		position.updateColor(request.colorType());

		return PositionResponse.from(position);
	}

	@Transactional
	public void deletePosition(
		String workspaceCode,
		Long positionId
	) {
		Position position = positionReader.findPosition(positionId, workspaceCode);

		positionValidator.validatePositionIsUsed(position);

		positionRepository.delete(position);
	}
}
