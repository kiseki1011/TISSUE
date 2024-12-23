package com.tissue.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.invitation.domain.Invitation;

public record AcceptInvitationResponse(
	Long invitationId,
	String workspaceCode,
	LocalDateTime acceptedAt
) {
	public static AcceptInvitationResponse from(Invitation invitation) {
		return new AcceptInvitationResponse(
			invitation.getId(),
			invitation.getWorkspaceCode(),
			invitation.getLastModifiedDate()
		);
	}
}
