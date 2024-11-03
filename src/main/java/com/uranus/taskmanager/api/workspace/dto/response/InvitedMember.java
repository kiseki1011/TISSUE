package com.uranus.taskmanager.api.workspace.dto.response;

import java.util.Objects;

import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		InvitedMember that = (InvitedMember)o;
		return Objects.equals(loginId, that.loginId) && Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(loginId, email);
	}
}
