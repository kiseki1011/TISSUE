package com.tissue.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;

public record InvitationResponse(
	Long invitationId,
	String workspaceCode,
	Long invitedBy,
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
