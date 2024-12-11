package com.uranus.taskmanager.api.position.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.position.domain.Position;
import com.uranus.taskmanager.api.position.domain.repository.PositionRepository;
import com.uranus.taskmanager.api.position.presentation.dto.response.GetPositionsResponse;

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
}
