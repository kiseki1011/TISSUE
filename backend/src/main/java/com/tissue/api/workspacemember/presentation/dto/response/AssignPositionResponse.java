package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.position.domain.Position;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record AssignPositionResponse(
	Long workspaceMemberId,
	String assignedPositionName,
	LocalDateTime assignedAt
) {
	public static AssignPositionResponse from(WorkspaceMember workspaceMember, Position position) {
		return new AssignPositionResponse(
			workspaceMember.getId(),
			position.getName(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
