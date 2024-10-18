package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class InvitedMember {
	private final String loginId;
	private final String email;

	@Builder
	public InvitedMember(String loginId, String email) {
		this.loginId = loginId;
		this.email = email;
	}

	public static InvitedMember from(Invitation invitation) {
		return InvitedMember.builder()
			.email(invitation.getMember().getEmail())
			.loginId(invitation.getMember().getLoginId())
			.build();
	}
}
