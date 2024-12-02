package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AcceptInvitationResponse {
	private Long invitationId;
	private String workspaceCode;
	private InvitationStatus status;
	private LocalDateTime acceptedAt;

	@Builder
	public AcceptInvitationResponse(
		Long invitationId,
		String workspaceCode,
		InvitationStatus status,
		LocalDateTime acceptedAt
	) {
		this.invitationId = invitationId;
		this.workspaceCode = workspaceCode;
		this.status = status;
		this.acceptedAt = acceptedAt;
	}

	public static AcceptInvitationResponse from(Invitation invitation) {
		return AcceptInvitationResponse.builder()
			.invitationId(invitation.getId())
			.workspaceCode(invitation.getWorkspaceCode())
			.status(invitation.getStatus())
			.acceptedAt(invitation.getLastModifiedDate())
			.build();
	}
}
