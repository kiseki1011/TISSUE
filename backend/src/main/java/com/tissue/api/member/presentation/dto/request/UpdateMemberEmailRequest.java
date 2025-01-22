package com.tissue.api.member.presentation.dto.request;

import com.tissue.api.common.validator.annotation.size.EmailSize;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateMemberEmailRequest(
	@NotBlank(message = "{valid.notblank}")
	String password,

	@EmailSize
	@Email(message = "{valid.pattern.email}")
	@NotBlank(message = "{valid.notblank}")
	String newEmail
) {
}
