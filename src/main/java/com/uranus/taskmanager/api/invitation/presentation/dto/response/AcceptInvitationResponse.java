package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AcceptInvitationResponse {
	private Long invitationId;
	private LocalDateTime acceptedAt;
	private String workspaceCode;

	@Builder
	public AcceptInvitationResponse(Long invitationId, LocalDateTime acceptedAt, String workspaceCode) {
		this.invitationId = invitationId;
		this.acceptedAt = acceptedAt;
		this.workspaceCode = workspaceCode;
	}

	public static AcceptInvitationResponse from(Invitation invitation, String code) {
		return AcceptInvitationResponse.builder()
			.invitationId(invitation.getId())
			.acceptedAt(invitation.getLastModifiedDate())
			.workspaceCode(code)
			.build();
	}
}
