package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record RemoveWorkspaceMemberResponse(
	Long removedWorkspaceMemberId,
	LocalDateTime removedAt
) {
	public static RemoveWorkspaceMemberResponse from(WorkspaceMember workspaceMember) {
		return new RemoveWorkspaceMemberResponse(
			workspaceMember.getId(),
			workspaceMember.getLastModifiedDate()
		);
	}
}
