package com.uranus.taskmanager.api.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupRequest {

	@NotBlank(message = "Login ID must not be blank")
	@Pattern(regexp = "^[a-zA-Z0-9]{2,20}$",
		message = "Login ID must be alphanumeric"
			+ " and must be between 2 and 20 characters")
	private final String loginId;

	@NotBlank(message = "Email must not be blank")
	@Size(min = 5, max = 254, message = "Email must be between 5 and 254 characters")
	@Email(message = "Email should be in a valid format")
	private final String email;

	@NotBlank(message = "Password must not be blank")
	@Pattern(regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric"
			+ " including at least one special character and must be between 8 and 30 characters")
	private final String password;
}
