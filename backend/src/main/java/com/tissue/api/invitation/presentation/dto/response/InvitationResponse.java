package com.tissue.api.invitation.presentation.dto.response;

import com.tissue.api.invitation.domain.model.Invitation;

public record InvitationResponse(
	String workspaceCode,
	Long invitationId
) {
	public static InvitationResponse from(Invitation invitation) {
		return new InvitationResponse(invitation.getWorkspaceCode(), invitation.getId());
	}
}
