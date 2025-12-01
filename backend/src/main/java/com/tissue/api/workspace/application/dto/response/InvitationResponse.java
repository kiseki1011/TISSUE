package com.tissue.api.workspace.application.dto.response;

import com.tissue.api.workspace.domain.Invitation;

public record InvitationResponse(
	String workspaceKey,
	Long invitationId
) {
	public static InvitationResponse from(Invitation invitation) {
		return new InvitationResponse(invitation.getWorkspaceKey(), invitation.getId());
	}
}
