package com.tissue.position.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.position.domain.model.Position;
import com.tissue.position.infrastructure.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionFinder {

	private final PositionRepository positionRepository;

	public Position findByIdAndWorkspaceKey(Long positionId, String workspaceCode) {
		return positionRepository.findByIdAndWorkspace_Key(positionId, workspaceCode)
			// TODO: PositionNotFoundException
			.orElseThrow(() -> new RuntimeException(String.format(
				"Position was not found with positionId: %d, workspaceKey: %s",
				positionId, workspaceCode)));
	}
}
