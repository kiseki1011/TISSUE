package com.uranus.taskmanager.api.security.authentication.presentation.dto.response;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Todo
 *  - 다음의 선택들을 고려 중
 *  - LoginResponse -> MemberSession 이름 변경, 그대로 사용
 *  - LoginResponse의 내용물을 MemberSession으로 포장하고 사용
 *  - 그냥 지금 처럼 LoginResponse에서 바로 꺼내서 세션에 저장
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginResponse {
	private Long id;
	private String loginId;
	private String email;

	@Builder
	public LoginResponse(Long id, String loginId, String email) {
		this.id = id;
		this.loginId = loginId;
		this.email = email;
	}

	public static LoginResponse from(Member member) {
		return LoginResponse.builder()
			.id(member.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.build();
	}
}
