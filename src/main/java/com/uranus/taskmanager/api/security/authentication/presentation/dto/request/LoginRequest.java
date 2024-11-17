package com.uranus.taskmanager.api.security.authentication.presentation.dto.request;

import com.uranus.taskmanager.api.member.domain.Member;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginRequest {

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
