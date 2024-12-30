package com.tissue.api.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateMemberNameRequest(
	@NotBlank(message = "First name must not be blank")
	@Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
	String firstName,

	@NotBlank(message = "Last name must not be blank")
	@Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
	String lastName
) {
}
