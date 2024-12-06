package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;

public record InvitationResponse(
	Long invitationId,
	String workspaceCode,
	String inviter,
	InvitationStatus status,
	LocalDateTime invitedAt
) {
	public static InvitationResponse from(Invitation invitation) {
		return new InvitationResponse(
			invitation.getId(),
			invitation.getWorkspaceCode(),
			invitation.getCreatedBy(),
			invitation.getStatus(),
			invitation.getCreatedDate()
		);
	}
}
