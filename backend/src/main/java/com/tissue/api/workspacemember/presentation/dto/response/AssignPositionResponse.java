package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.position.presentation.dto.response.PositionDetail;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record AssignPositionResponse(
	Long workspaceMemberId,
	List<PositionDetail> assignedPositions,
	LocalDateTime assignedAt
) {
	public static AssignPositionResponse from(WorkspaceMember workspaceMember) {
		List<PositionDetail> positionDetails = workspaceMember.getWorkspaceMemberPositions().stream()
			.map(wmp -> PositionDetail.from(wmp.getPosition()))
			.toList();

		return new AssignPositionResponse(
			workspaceMember.getId(),
			positionDetails,
			workspaceMember.getLastModifiedDate()
		);
	}
}
