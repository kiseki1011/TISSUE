package com.tissue.api.invitation.presentation.dto.response;

import java.time.Instant;

import com.tissue.api.invitation.domain.enums.InvitationStatus;
import com.tissue.api.invitation.domain.model.Invitation;

import lombok.Builder;

@Builder
public record InvitationDetail(
	Long invitationId,
	String workspaceCode,
	Long invitedBy,
	InvitationStatus status,
	Instant invitedAt
) {
	public static InvitationDetail from(Invitation invitation) {
		return InvitationDetail.builder()
			.invitationId(invitation.getId())
			.workspaceCode(invitation.getWorkspaceCode())
			.invitedBy(invitation.getCreatedBy())
			.status(invitation.getStatus())
			.invitedAt(invitation.getCreatedDate())
			.build();
	}
}
