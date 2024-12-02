package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RejectInvitationResponse {
	private Long invitationId;
	private LocalDateTime rejectedAt;

	@Builder
	public RejectInvitationResponse(Long invitationId, LocalDateTime rejectedAt) {
		this.invitationId = invitationId;
		this.rejectedAt = rejectedAt;
	}

	public static RejectInvitationResponse from(Invitation invitation) {
		return RejectInvitationResponse.builder()
			.invitationId(invitation.getId())
			.rejectedAt(invitation.getLastModifiedDate())
			.build();
	}
}
