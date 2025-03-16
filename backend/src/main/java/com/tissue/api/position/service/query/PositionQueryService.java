package com.tissue.api.position.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.position.domain.Position;
import com.tissue.api.position.domain.repository.PositionQueryRepository;
import com.tissue.api.position.presentation.dto.response.GetPositionsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionQueryService {

	private final PositionQueryRepository positionQueryRepository;

	@Transactional(readOnly = true)
	public GetPositionsResponse getPositions(String workspaceCode) {

		List<Position> positions = positionQueryRepository.findAllByWorkspaceCodeOrderByCreatedDateAsc(workspaceCode);

		return GetPositionsResponse.from(positions);
	}
}
