package com.tissue.api.workspace.application.dto.response;

import java.time.Instant;

import com.tissue.api.workspace.domain.Invitation;
import com.tissue.api.workspace.domain.enums.InvitationStatus;

import lombok.Builder;

@Builder
public record InvitationDetail(
	Long invitationId,
	String workspaceKey,
	InvitationStatus status,
	Long invitedBy,
	Instant invitedAt
) {
	public static InvitationDetail from(Invitation invitation) {
		return InvitationDetail.builder()
			.invitationId(invitation.getId())
			.workspaceKey(invitation.getWorkspaceKey())
			.status(invitation.getStatus())
			.invitedBy(invitation.getCreatedBy())
			.invitedAt(invitation.getCreatedAt())
			.build();
	}
}
