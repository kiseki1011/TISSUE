package com.uranus.taskmanager.api.position.presentation.dto.response;

import java.util.List;

import com.uranus.taskmanager.api.position.domain.Position;

public record GetPositionsResponse(
	List<PositionDetail> positions
) {
	public static GetPositionsResponse from(List<Position> positions) {
		List<PositionDetail> responses = positions.stream()
			.map(PositionDetail::from)
			.toList();
		return new GetPositionsResponse(responses);
	}
}
