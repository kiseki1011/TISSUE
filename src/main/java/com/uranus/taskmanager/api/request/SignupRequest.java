package com.uranus.taskmanager.api.request;

import com.uranus.taskmanager.api.domain.member.Member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SignupRequest {

	@NotBlank(message = "User ID must not be blank")
	@Pattern(regexp = "^[a-zA-Z0-9]{2,20}$",
		message = "User ID must be alphanumeric"
			+ " and must be between 2 and 20 characters")
	private final String userId;

	@NotBlank(message = "Email must not be blank")
	@Email(message = "Email should be valid")
	private final String email;

	@NotBlank(message = "Password must not be blank")
	@Pattern(regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric"
			+ " including at least one special character and must be between 8 and 30 characters")
	private final String password;

	@Builder
	public SignupRequest(String userId, String email, String password) {
		this.userId = userId;
		this.email = email;
		this.password = password;
	}

	public Member toEntity() {
		return Member.builder()
			.userId(userId)
			.email(email)
			.password(password)
			.build();
	}
}
