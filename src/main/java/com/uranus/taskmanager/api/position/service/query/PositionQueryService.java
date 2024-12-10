package com.uranus.taskmanager.api.position.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.position.domain.Position;
import com.uranus.taskmanager.api.position.domain.repository.PositionRepository;
import com.uranus.taskmanager.api.position.presentation.dto.response.GetPositionsResponse;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionQueryService {

	private final PositionRepository positionRepository;
	private final WorkspaceRepository workspaceRepository;

	@Transactional(readOnly = true)
	public GetPositionsResponse getPositions(String workspaceCode) {
		// 워크스페이스 존재 여부 확인
		if (!workspaceRepository.existsByCode(workspaceCode)) {
			throw new WorkspaceNotFoundException();
		}

		// 해당 워크스페이스의 모든 Position 조회
		List<Position> positions = positionRepository.findAllByWorkspaceCodeOrderByCreatedDateAsc(workspaceCode);

		log.debug("Positions retrieved: count={}, workspace={}", positions.size(), workspaceCode);

		return GetPositionsResponse.from(positions);
	}
}
