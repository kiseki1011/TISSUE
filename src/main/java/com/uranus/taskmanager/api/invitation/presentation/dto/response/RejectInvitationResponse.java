package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RejectInvitationResponse {
	private Long invitationId;
	private LocalDateTime rejectedAt;
	private String workspaceCode;

	@Builder
	public RejectInvitationResponse(Long invitationId, LocalDateTime rejectedAt, String workspaceCode) {
		this.invitationId = invitationId;
		this.rejectedAt = rejectedAt;
		this.workspaceCode = workspaceCode;
	}

	public static RejectInvitationResponse from(Invitation invitation, String code) {
		return RejectInvitationResponse.builder()
			.invitationId(invitation.getId())
			.workspaceCode(code)
			.rejectedAt(invitation.getLastModifiedDate())
			.build();
	}
}
