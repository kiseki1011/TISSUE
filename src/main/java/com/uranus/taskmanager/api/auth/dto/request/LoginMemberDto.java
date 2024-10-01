package com.uranus.taskmanager.api.auth.dto.request;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

/**
 * Todo: 추후에 nickname 추가가 필요하면 추가
 */
@Getter
public class LoginMemberDto {

	private final String loginId;
	private final String email;

	@Builder
	public LoginMemberDto(String loginId, String email) {
		this.loginId = loginId;
		this.email = email;
	}

	public static LoginMemberDto fromEntity(Member member) {
		return LoginMemberDto.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
