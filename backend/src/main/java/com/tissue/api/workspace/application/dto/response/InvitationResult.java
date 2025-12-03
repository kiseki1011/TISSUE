package com.tissue.api.workspace.application.dto.response;

import com.tissue.api.workspace.domain.Invitation;

public record InvitationResult(
	String workspaceKey,
	Long invitationId
) {
	public static InvitationResult from(Invitation invitation) {
		return new InvitationResult(invitation.getWorkspaceKey(), invitation.getId());
	}
}
