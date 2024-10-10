package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class InviteMemberResponse {

	private final String code;
	private final String loginId;
	private final String email;
	private final InvitationStatus status;

	@Builder
	public InviteMemberResponse(String code, String loginId, String email, InvitationStatus status) {
		this.code = code;
		this.loginId = loginId;
		this.email = email;
		this.status = status;
	}

	public static InviteMemberResponse fromEntity(Invitation invitation) {
		return InviteMemberResponse.builder()
			.code(invitation.getWorkspace().getCode())
			.loginId(invitation.getMember().getLoginId())
			.email(invitation.getMember().getEmail())
			.status(invitation.getStatus())
			.build();
	}

}
