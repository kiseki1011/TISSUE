package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.invitation.domain.Invitation;

public record RejectInvitationResponse(
	Long invitationId,
	String workspaceCode,
	LocalDateTime rejectedAt
) {
	public static RejectInvitationResponse from(Invitation invitation) {
		return new RejectInvitationResponse(
			invitation.getId(),
			invitation.getWorkspaceCode(),
			invitation.getLastModifiedDate()
		);
	}
}
