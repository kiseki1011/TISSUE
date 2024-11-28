package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SignupMemberResponse {

	private Long memberId;
	private LocalDateTime createdAt;

	@Builder
	public SignupMemberResponse(Long memberId, LocalDateTime createdAt) {
		this.memberId = memberId;
		this.createdAt = createdAt;
	}

	public static SignupMemberResponse from(Member member) {
		return SignupMemberResponse.builder()
			.memberId(member.getId())
			.createdAt(member.getCreatedDate())
			.build();
	}
}
