package com.tissue.api.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateMemberNameRequest {

	@NotBlank(message = "First name must not be blank")
	@Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
	private String firstName;

	@NotBlank(message = "Last name must not be blank")
	@Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
	private String lastName;

	@Builder
	public UpdateMemberNameRequest(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
}
