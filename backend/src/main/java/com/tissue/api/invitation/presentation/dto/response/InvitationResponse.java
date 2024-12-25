package com.tissue.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;

import lombok.Builder;

@Builder
public record InvitationResponse(
	Long invitationId,
	String workspaceCode,
	Long invitedBy,
	InvitationStatus status,
	LocalDateTime invitedAt
) {
	public static InvitationResponse from(Invitation invitation) {
		return InvitationResponse.builder()
			.invitationId(invitation.getId())
			.workspaceCode(invitation.getWorkspaceCode())
			.invitedBy(invitation.getCreatedBy())
			.status(invitation.getStatus())
			.invitedAt(invitation.getCreatedDate())
			.build();
	}
}
