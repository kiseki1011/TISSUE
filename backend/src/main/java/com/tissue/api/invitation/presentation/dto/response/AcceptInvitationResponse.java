package com.tissue.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.invitation.domain.Invitation;

import lombok.Builder;

@Builder
public record AcceptInvitationResponse(
	Long invitationId,
	String workspaceCode,
	LocalDateTime acceptedAt
) {
	public static AcceptInvitationResponse from(Invitation invitation) {
		return AcceptInvitationResponse.builder()
			.invitationId(invitation.getId())
			.workspaceCode(invitation.getWorkspaceCode())
			.acceptedAt(invitation.getLastModifiedDate())
			.build();
	}
}
