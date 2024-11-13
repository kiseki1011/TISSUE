package com.uranus.taskmanager.api.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberEmailUpdateRequest {

	@NotBlank(message = "Email must not be blank")
	@Size(min = 5, max = 254, message = "Email must be between 5 and 254 characters")
	@Email(message = "Email should be in a valid format")
	private String updateEmail;
}
