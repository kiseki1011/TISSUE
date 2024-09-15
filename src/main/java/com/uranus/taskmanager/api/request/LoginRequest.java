package com.uranus.taskmanager.api.request;

import com.uranus.taskmanager.api.domain.member.Member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LoginRequest {

	@NotBlank(message = "Email must not be blank")
	@Email(message = "Email should be valid")
	private String email;

	@NotBlank(message = "Password must not be blank")
	private String password;

	@Builder
	public LoginRequest(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public Member toEntity() {
		return Member.builder()
			.email(email)
			.password(password)
			.build();
	}

}
