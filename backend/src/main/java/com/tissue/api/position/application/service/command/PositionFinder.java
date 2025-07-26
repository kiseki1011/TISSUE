package com.tissue.api.position.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.position.infrastructure.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionFinder {

	private final PositionRepository positionRepository;

	public Position findPosition(Long positionId, String workspaceCode) {
		return positionRepository.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				"Position was not found with positionId: %d, workspaceCode: %s",
				positionId, workspaceCode)));
	}
}
