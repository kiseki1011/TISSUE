package com.tissue.api.position.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.position.domain.Position;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.presentation.dto.response.GetPositionsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionQueryService {

	private final PositionRepository positionRepository;

	@Transactional(readOnly = true)
	public GetPositionsResponse getPositions(String workspaceCode) {

		List<Position> positions = positionRepository.findAllByWorkspaceCodeOrderByCreatedDateAsc(workspaceCode);

		return GetPositionsResponse.from(positions);
	}

	@Transactional(readOnly = true)
	public Position findPosition(Long positionId, String workspaceCode) {
		return positionRepository.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				"Position was not found with positionId: %d, workspaceCode: %s",
				positionId, workspaceCode)));
	}
}
