package com.tissue.api.security.authentication.presentation.dto.request;

import com.tissue.api.member.domain.Member;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginRequest {

	/**
	 * Todo
	 *  - NotBlank, Size 검증 필요
	 */
	private String email;
	private String loginId;

	@NotBlank(message = "Password must not be blank")
	private String password;

	public Member to() {
		return Member.builder()
			.email(email)
			.password(password)
			.build();
	}

}
