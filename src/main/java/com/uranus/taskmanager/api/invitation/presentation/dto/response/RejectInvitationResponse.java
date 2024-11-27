package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RejectInvitationResponse {
	private Long invitationId;
	private LocalDateTime rejectedAt;
	private String code;

	@Builder
	public RejectInvitationResponse(Long invitationId, LocalDateTime rejectedAt, String code) {
		this.invitationId = invitationId;
		this.rejectedAt = rejectedAt;
		this.code = code;
	}

	public static RejectInvitationResponse from(Invitation invitation, String code) {
		return RejectInvitationResponse.builder()
			.invitationId(invitation.getId())
			.code(code)
			.rejectedAt(invitation.getLastModifiedDate())
			.build();
	}
}
