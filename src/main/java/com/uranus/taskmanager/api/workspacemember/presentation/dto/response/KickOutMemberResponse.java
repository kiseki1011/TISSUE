package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class KickOutMemberResponse {

	private String memberIdentifier;
	private String nickname;
	private LocalDateTime kickedOutAt;

	@Builder
	public KickOutMemberResponse(String memberIdentifier, String nickname) {
		this.memberIdentifier = memberIdentifier;
		this.nickname = nickname;
		this.kickedOutAt = LocalDateTime.now();
	}

	public static KickOutMemberResponse from(String memberIdentifier, WorkspaceMember workspaceMember) {
		return KickOutMemberResponse.builder()
			.memberIdentifier(memberIdentifier)
			.nickname(workspaceMember.getNickname())
			.build();
	}
}
