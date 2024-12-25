package com.tissue.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.invitation.domain.Invitation;

import lombok.Builder;

@Builder
public record RejectInvitationResponse(
	Long invitationId,
	String workspaceCode,
	LocalDateTime rejectedAt
) {
	public static RejectInvitationResponse from(Invitation invitation) {
		return RejectInvitationResponse.builder()
			.invitationId(invitation.getId())
			.workspaceCode(invitation.getWorkspaceCode())
			.rejectedAt(invitation.getLastModifiedDate())
			.build();
	}
}
