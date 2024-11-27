package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
public class InviteMemberResponse {

	private final String code;
	private final InvitationStatus status;
	private final InvitedMember invitedMember;

	@Builder
	public InviteMemberResponse(String code, InvitationStatus status, InvitedMember invitedMember) {
		this.code = code;
		this.status = status;
		this.invitedMember = invitedMember;
	}

	public static InviteMemberResponse from(Invitation invitation) {
		return InviteMemberResponse.builder()
			.code(invitation.getWorkspace().getCode())
			.invitedMember(InvitedMember.from(invitation))
			.status(invitation.getStatus())
			.build();
	}
}
